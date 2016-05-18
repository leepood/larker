#include "larker_jni.h"
#include "log.h"

Larker* getLarkerInstance(JNIEnv *env, jobject jthiz)
{
	const char* fieldName = "mNativeInstance";
	jclass cls = env->GetObjectClass(jthiz);
	jfieldID instanceFieldId = env->GetFieldID(cls, fieldName, "J");
	if(instanceFieldId == NULL){
		LOGE("LarkerNative has no long field named with: mNativeInstance");
		return NULL;
	}
	jlong instanceValue = env->GetLongField(jthiz,instanceFieldId);
	if(instanceValue == 0) 
	{
		LOGE("instanceValue NULL ");
		return NULL;
	}
	else{
		LOGE("instanceValue NOT NULL ");
	}
	return (Larker*)instanceValue;
}

void storeLarkerInstance(JNIEnv *env, jobject jthiz,Larker* instance)
{
	const char* fieldName = "mNativeInstance";
	jclass cls = env->GetObjectClass(jthiz);
	jfieldID instanceFieldId = env->GetFieldID(cls, fieldName, "J");
	if(instanceFieldId == NULL){
		LOGE("LarkerNative has no long field named with: mNativeInstance");
		return;
	}
	jlong value = (instance == NULL) ? 0 : (jlong)instance;
	env->SetLongField(jthiz,instanceFieldId,value);
}


JNIEXPORT jboolean JNICALL Java_com_leepood_lark_utils_LarkerNative_init
  (JNIEnv *env, jobject jthiz,jint fps,jint port)
 {
    Larker* mLarker = new Larker(fps,port);
    storeLarkerInstance(env,jthiz,mLarker);
    return mLarker->init();
 }

JNIEXPORT void JNICALL Java_com_leepood_lark_utils_LarkerNative_loopNative
  (JNIEnv *env, jobject jthiz)
  {
  		Larker* larker = getLarkerInstance(env,jthiz);
  		if(larker != NULL) larker->loop();
  }


JNIEXPORT void JNICALL Java_com_leepood_lark_utils_LarkerNative_stopNative
  (JNIEnv *env, jobject jthiz)
  {
  		Larker* larker = getLarkerInstance(env,jthiz);
  		if(larker != NULL) larker->stop();
  }


JNIEXPORT void JNICALL Java_com_leepood_lark_utils_LarkerNative_destory
  (JNIEnv *env, jobject jthiz)
 {
 	Larker* larker = getLarkerInstance(env,jthiz);
 	if(larker != NULL){
 		delete larker;
 		larker = 0;
 		storeLarkerInstance(env,jthiz,NULL);
 	}
 }

JNIEXPORT void JNICALL Java_com_leepood_lark_utils_LarkerNative_feedH264Data
  (JNIEnv *env, jobject jthiz, jbyteArray dataArray)
  {
  	Larker* larker = getLarkerInstance(env,jthiz);
  	if(larker == NULL) return;
  	int len = env->GetArrayLength(dataArray);
    char* buf = new char[len];
    env->GetByteArrayRegion (dataArray, 0, len, reinterpret_cast<jbyte*>(buf));
   	larker->dataPushed(buf,len);
  }