/*
 * Copyright (C) 2018 OBONO
 * http://d.hatena.ne.jp/OBONO/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


#include <stdbool.h>
#include <android/log.h>

#define OLED_WIDTH_PX (128)
#define OLED_HEIGHT_PX (64)

#define LOG_TAG "ArbyEmulator"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

enum button_e {
	BTN_UP = 0,
	BTN_DOWN,
	BTN_LEFT,
	BTN_RIGHT,
	BTN_A,
	BTN_B,
	BTN_COUNT,
};

enum led_e {
	LED_RED = 0,
	LED_GREEN,
	LED_BLUE,
	LED_RX,
	LED_TX,
	LED_COUNT,
};

int arduboy_avr_setup(const char *hex_file_path, bool is_tuned);
bool arduboy_avr_get_eeprom(char *p_array);
bool arduboy_avr_set_eeprom(const char *p_array);
bool arduboy_avr_set_refresh_timing(bool is_postpone);
bool arduboy_avr_button_event(enum button_e btn_e, bool pressed);
bool arduboy_avr_loop(int *pixels);
bool arduboy_avr_get_led_state(int *leds);
void arduboy_avr_teardown(void);
