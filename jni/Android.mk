LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := ArtemisMessenger
LOCAL_SRC_FILES := ArtemisMessenger.cpp

include $(BUILD_SHARED_LIBRARY)
