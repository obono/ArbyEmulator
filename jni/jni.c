/*
	Copyright (C) 2017 OBONO

	Arduboy emulator using simavr on Android platform.

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

#include <stdio.h>

#include "arduboy_avr.h"

#include "com_obnsoft_arduboyemu_Native.h"

/*
 * Class:     com_obnsoft_arduboyemu_Native
 * Method:    setup
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_obnsoft_arduboyemu_Native_setup(
        JNIEnv *env, jclass obj, jstring js_path) {
    int ret;
    const char *path = (*env)->GetStringUTFChars(env, js_path, NULL);

    ret = arduboy_avr_setup(path);

    (*env)->ReleaseStringUTFChars(env, js_path, path);
    return !ret;
}

/*
 * Class:     com_obnsoft_arduboyemu_Native
 * Method:    buttonEvent
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_com_obnsoft_arduboyemu_Native_buttonEvent(
        JNIEnv *env, jclass obj, jint key, jboolean is_press) {
    arduboy_avr_button_event((enum button_e) key, is_press);
}

/*
 * Class:     com_obnsoft_arduboyemu_Native
 * Method:    loop
 * Signature: ([I)Z
 */
JNIEXPORT jboolean JNICALL Java_com_obnsoft_arduboyemu_Native_loop(
        JNIEnv *env, jclass obj, jintArray jint_array) {
    jboolean ret;
    jint *p_array = (*env)->GetIntArrayElements(env, jint_array, &ret);
    int array_len = (*env)->GetArrayLength(env, jint_array);

    if (array_len == OLED_WIDTH_PX * OLED_HEIGHT_PX) {
        ret = arduboy_avr_loop(p_array);
    } else {
        ret = JNI_FALSE;
    }

    (*env)->ReleaseIntArrayElements(env, jint_array, p_array, 0);
    return ret;
}

/*
 * Class:     com_obnsoft_arduboyemu_Native
 * Method:    teardown
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_obnsoft_arduboyemu_Native_teardown(
        JNIEnv *env, jclass obj) {
    arduboy_avr_teardown();
}

