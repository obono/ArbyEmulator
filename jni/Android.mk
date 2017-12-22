##
##  Build JNI library
##
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# Source files to build
LOCAL_SRC_FILES := \
	simavr/simavr/sim/avr_acomp.c \
	simavr/simavr/sim/avr_adc.c \
	simavr/simavr/sim/avr_bitbang.c \
	simavr/simavr/sim/avr_eeprom.c \
	simavr/simavr/sim/avr_extint.c \
	simavr/simavr/sim/avr_flash.c \
	simavr/simavr/sim/avr_ioport.c \
	simavr/simavr/sim/avr_lin.c \
	simavr/simavr/sim/avr_spi.c \
	simavr/simavr/sim/avr_timer.c \
	simavr/simavr/sim/avr_twi.c \
	simavr/simavr/sim/avr_uart.c \
	simavr/simavr/sim/avr_usb.c \
	simavr/simavr/sim/avr_watchdog.c \
	simavr/simavr/sim/run_avr.c \
	simavr/simavr/sim/sim_avr.c \
	simavr/simavr/sim/sim_cmds.c \
	simavr/simavr/sim/sim_core.c \
	simavr/simavr/sim/sim_cycle_timers.c \
	simavr/simavr/sim/sim_elf.c \
	simavr/simavr/sim/sim_gdb.c \
	simavr/simavr/sim/sim_hex.c \
	simavr/simavr/sim/sim_interrupts.c \
	simavr/simavr/sim/sim_io.c \
	simavr/simavr/sim/sim_irq.c \
	simavr/simavr/sim/sim_utils.c \
	simavr/simavr/sim/sim_vcd_file.c \
	simavr/simavr/cores/sim_mega32u4.c \
	simavr/examples/parts/ssd1306_virt.c \
	jni.c \
	arduboy_avr.c

# Include JNI headers
LOCAL_C_INCLUDES += \
	$(LOCAL_PATH) \
	$(LOCAL_PATH)/elfutils/0.153/libelf \
	$(LOCAL_PATH)/simavr/simavr/cores \
	$(LOCAL_PATH)/simavr/simavr/sim \
	$(LOCAL_PATH)/simavr/examples/parts

# C flags
#LOCAL_CFLAGS += \
	-Wunused-parameter \
	-Wmissing-field-initializers \
	-Wuninitialized \
	-std=c99

# LD libraries
LOCAL_LDLIBS += \
	-llog

# Name of the library to build
LOCAL_MODULE := libArduboyEmulatorNative

# Necessary libraries
LOCAL_STATIC_LIBRARIES += \
	libelf

# Tell it to build a shared library
include $(BUILD_SHARED_LIBRARY)

##
##  Call other Android.mk
##
include $(LOCAL_PATH)/elfutils/0.153/libelf/Android.mk
