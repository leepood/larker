#include "larker.h"
#include "DisplayDeviceSource.h"

#include <GroupsockHelper.hh>
#include <BasicUsageEnvironment.hh>

#include "H264_DisplayDeviceSource.h"
#include "ServerMediaSubsession.h"

// static Larker* mInstance = NULL;

// Larker* Larker::getInstance(){

// 	if(mInstance == NULL){
// 		mInstance = new Larker();
// 	}
// 	return mInstance;
// }

Larker::Larker(unsigned int _fps,unsigned int _port){
	queueSize = 40;
	fps = _fps;
	rtpPortNum = 20000;
	rtcpPortNum = rtpPortNum+1;
	ttl = 5;
	rtspPort = _port;
	rtspOverHTTPPort = 0;
	multicast = false;
	useMmap = true;
	url = "larker";
	murl = "larker";
	useThread = true;
	maddr = INADDR_NONE;
	repeatConfig = true;
	timeout = 65;
	_authDB = NULL;
	quit = 0;
}


Larker::~Larker(){

}

void Larker::addSession(RTSPServer* rtspServer, const char* sessionName, ServerMediaSubsession *subSession)
{
	UsageEnvironment& env(rtspServer->envir());
	ServerMediaSession* sms = ServerMediaSession::createNew(env, sessionName);
	sms->addSubsession(subSession);
	rtspServer->addServerMediaSession(sms);

	char* url = rtspServer->rtspURL(sms);
	LOGE("Play this stream using the URL : %s",url);
	delete[] url;			
}


bool Larker::init(){
	_scheduler 		= BasicTaskScheduler::createNew();
	_env 			= BasicUsageEnvironment::createNew(*_scheduler);
	_rtspServer 	= RTSPServer::createNew(*_env, rtspPort, _authDB, timeout);
	
	if(_rtspServer == NULL)
	{
		LOGE("create rtsp server failed");
		return false;
	}
	else
	{
		// if (rtspOverHTTPPort)
		// {
		// 	_rtspServer->setUpTunnelingOverHTTP(rtspOverHTTPPort);
		// }	
		_displaySource = H264_DisplayDeviceSource::createNew(*_env, queueSize, useThread, repeatConfig);
		if(_displaySource == NULL)
		{
			LOGE("unable to create source for device");
			return false;
		}
		else
		{
			OutPacketBuffer::maxSize = 600000;//DisplayDeviceSource::bufferedSize;
			StreamReplicator* replicator = StreamReplicator::createNew(*_env, _displaySource, false);
			if (multicast)
			{
				if (maddr == INADDR_NONE) maddr = chooseRandomIPv4SSMAddress(*_env);	
				destinationAddress.s_addr = maddr;
				LOGE("RTP  address :%s:%d", inet_ntoa(destinationAddress),rtpPortNum);
				LOGE("RTCP address :%s:%d", inet_ntoa(destinationAddress),rtcpPortNum);
				addSession(_rtspServer, murl.c_str(), MulticastServerMediaSubsession::createNew(*_env,destinationAddress,
					 Port(rtpPortNum), Port(rtcpPortNum), ttl, replicator));
			}
			addSession(_rtspServer, url.c_str(), UnicastServerMediaSubsession::createNew(*_env,replicator));		
		}
	}
	return true;
}

// This function will block current thread
void Larker::loop()
{
	LOGE("START LOOP");
	_env->taskScheduler().doEventLoop(&quit); 
	LOGE("END LOOP");
	Medium::close(_displaySource);
	_env->reclaim();
	delete _scheduler;
}


void Larker::dataPushed(char* data,unsigned int dataSize)
{
	LOGE("push  raw data\t dataSize:%d",dataSize);
	_displaySource->pushRawData(data,dataSize);
}

void Larker::stop()
{
	LOGE("STOP....");	
	quit = 1;
}