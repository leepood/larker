LOCAL_PATH := $(call my-dir)  


include $(CLEAR_VARS)  
  
LOCAL_MODULE := larker

SOURCE_LIST := $(wildcard $(LOCAL_PATH)/BasicUsageEnvironment/*.cpp)
SOURCE_LIST += $(wildcard $(LOCAL_PATH)/groupsock/*.cpp)
SOURCE_LIST += $(wildcard $(LOCAL_PATH)/groupsock/*.c)
SOURCE_LIST += $(wildcard $(LOCAL_PATH)/liveMedia/*.cpp)
SOURCE_LIST += $(wildcard $(LOCAL_PATH)/liveMedia/*.c)
SOURCE_LIST += $(wildcard $(LOCAL_PATH)/UsageEnvironment/*.cpp)

LOCAL_SRC_FILES := $(SOURCE_LIST)

LOCAL_SRC_FILES += larker/DisplayDeviceSource.cpp \
				   larker/H264_DisplayDeviceSource.cpp \
				   larker/ServerMediaSubsession.cpp \
				   larker/larker.cpp \
				   larker/larker_jni.cpp

LOCAL_C_INCLUDES := BasicUsageEnvironment/include \
					liveMedia/include \
					groupsock/include \
					UsageEnvironment/include  


LOCAL_CPPFLAGS += -fexceptions -DXLOCALE_NOT_USED=1 -DNULL=0 -DNO_SSTREAM=1 -UIP_ADD_SOURCE_MEMBERSHIP

LOCAL_LDLIBS :=  -llog

include $(BUILD_SHARED_LIBRARY) 
