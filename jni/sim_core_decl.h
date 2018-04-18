/*
 * Arduboy emulator using simavr on Android platform.
 *
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

#ifndef __SIM_CORE_DECL_H__
#define __SIM_CORE_DECL_H__

#define CONFIG_SIMAVR_VERSION "v1.5"

extern avr_kind_t mega32u4;
extern avr_kind_t *avr_kind[];

#ifdef AVR_KIND_DECL
avr_kind_t *avr_kind[] = {
	&mega32u4,
	NULL
};
#endif
#endif
