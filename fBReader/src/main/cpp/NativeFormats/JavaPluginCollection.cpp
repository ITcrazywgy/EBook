

#include <vector>

#include <AndroidUtil.h>
#include <JniEnvelope.h>

#include "fbreader/src/formats/FormatPlugin.h"

extern "C"
JNIEXPORT jobjectArray JNICALL Java_org_geometerplus_fbreader_formats_PluginCollection_nativePlugins(JNIEnv* env, jobject thiz, jobject systemInfo) {
	const std::vector<shared_ptr<FormatPlugin> > plugins = PluginCollection::Instance().plugins();
	const std::size_t size = plugins.size();
	jclass cls = AndroidUtil::Class_NativeFormatPlugin.j();
	// TODO: memory leak?
	jobjectArray javaPlugins = env->NewObjectArray(size, cls, 0);

	for (std::size_t i = 0; i < size; ++i) {
		jstring fileType = AndroidUtil::createJavaString(env, plugins[i]->supportedFileType());
		jobject p = AndroidUtil::StaticMethod_NativeFormatPlugin_create->call(systemInfo, fileType);
		env->SetObjectArrayElement(javaPlugins, i, p);
		env->DeleteLocalRef(p);
		env->DeleteLocalRef(fileType);
	}
	return javaPlugins;
}

extern "C"
JNIEXPORT void JNICALL Java_org_geometerplus_fbreader_formats_PluginCollection_free(JNIEnv* env, jobject thiz) {
	PluginCollection::deleteInstance();
}
