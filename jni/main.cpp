/* ---------------------------------------------------------------------------
** This software is in the public domain, furnished "as is", without technical
** support, and with no warranty, express or implied, as to its usefulness for
** any purpose.
**
** main.cpp
** 
** V4L2 RTSP streamer                                                                 
**                                                                                    
** H264 capture using V4L2                                                            
** RTSP using live555                                                                 
**                                                                                    
** -------------------------------------------------------------------------*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <signal.h>

#include <sstream>


// live555
#include <BasicUsageEnvironment.hh>
#include <GroupsockHelper.hh>

// project
#include "log.h"

#include "H264_DisplayDeviceSource.h"
#include "ServerMediaSubsession.h"

// -----------------------------------------
//    signal handler
// -----------------------------------------
char quit = 0;
void sighandler(int n)
{ 
	printf("SIGINT\n");
	quit =1;
}

// -----------------------------------------
//    add an RTSP session
// -----------------------------------------
void addSession(RTSPServer* rtspServer, const char* sessionName, ServerMediaSubsession *subSession)
{
	UsageEnvironment& env(rtspServer->envir());
	ServerMediaSession* sms = ServerMediaSession::createNew(env, sessionName);
	sms->addSubsession(subSession);
	rtspServer->addServerMediaSession(sms);

	char* url = rtspServer->rtspURL(sms);
	LOG(NOTICE) << "Play this stream using the URL \"" << url << "\"";
	delete[] url;			
}

	
// -----------------------------------------
//    entry point
// -----------------------------------------
int main(int argc, char** argv) 
{
	// default parameters

	int queueSize = 10;
	int fps = 25;
	unsigned short rtpPortNum = 20000;
	unsigned short rtcpPortNum = rtpPortNum+1;
	unsigned char ttl = 5;
	struct in_addr destinationAddress;
	unsigned short rtspPort = 8554;
	unsigned short rtspOverHTTPPort = 0;
	bool multicast = false;
	int verbose = 0;
	std::string outputFile;
	bool useMmap = true;
	std::string url = "unicast";
	std::string murl = "multicast";
	bool useThread = true;
	in_addr_t maddr = INADDR_NONE;
	bool repeatConfig = true;
	int timeout = 65;

	// decode parameters   
     
	// create live555 environment
	TaskScheduler* scheduler = BasicTaskScheduler::createNew();
	UsageEnvironment* env = BasicUsageEnvironment::createNew(*scheduler);	
	
	// create RTSP server
	UserAuthenticationDatabase* authDB = NULL;
	RTSPServer* rtspServer = RTSPServer::createNew(*env, rtspPort, authDB, timeout);
	if (rtspServer == NULL) 
	{
		LOGE("Failed to create RTSP server:%s",env->getResultMsg());
	}
	else
	{
		// set http tunneling
		if (rtspOverHTTPPort)
		{
			rtspServer->setUpTunnelingOverHTTP(rtspOverHTTPPort);
		}
		

		DisplayDeviceSource* videoES =  H264_DisplayDeviceSource::createNew(*env, queueSize, useThread, repeatConfig);
		if (videoES == NULL) 
		{
			LOGE("Unable to create source for device");
		}
		else
		{
			OutPacketBuffer::maxSize = 10000;
			StreamReplicator* replicator = StreamReplicator::createNew(*env, videoES, false);

			// Create Server Multicast Session
			if (multicast)
			{
				if (maddr == INADDR_NONE) maddr = chooseRandomIPv4SSMAddress(*env);	
				destinationAddress.s_addr = maddr;
				LOGE("RTP  address :%s:%d", inet_ntoa(destinationAddress),rtpPortNum);
				LOGE("RTCP address :%s:%d", inet_ntoa(destinationAddress),rtcpPortNum);
				addSession(rtspServer, murl.c_str(), MulticastServerMediaSubsession::createNew(*env,destinationAddress,
					 Port(rtpPortNum), Port(rtcpPortNum), ttl, replicator));
			
			}
			// Create Server Unicast Session
			addSession(rtspServer, url.c_str(), UnicastServerMediaSubsession::createNew(*env,replicator));

			// main loop
			signal(SIGINT,sighandler);
			env->taskScheduler().doEventLoop(&quit); 
			LOGE("Exiting....");			
			Medium::close(videoES);
		}			
		
	
	Medium::close(rtspServer);
	
	
	env->reclaim();
	delete scheduler;	
	
	return 0;
}



