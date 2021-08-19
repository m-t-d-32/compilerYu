# directories
LIB_DIR := $(USR_DIR)/lib
BIN_DIR := $(USR_DIR)/bin
BIN_OBJ_DIR := $(OBJ_DIR)/usr/bin
BIN_TARGET_DIR := $(BUILD_DIR)/usr

# sources & targets of library
LIB_SRC := $(call rwildcard, $(LIB_DIR), *.yu)
LIB_SRC += $(call rwildcard, $(LIB_DIR), *.c)
LIB_SRC += $(call rwildcard, $(LIB_DIR), *.S)
$(call make_obj, LIB, $(LIB_SRC))
# 于是 LIB_OBJ 代表 ./usr/lib 中的那些文件名，加上后缀.o 改到相对位置为 ./build/obj/usr/lib 中的那些文件名
LIB_TARGET := $(BUILD_DIR)/libgrt.a

# sources & targets of user binaries
BIN_SRC := $(call rwildcard, $(BIN_DIR), *.yu)
$(call make_obj, BIN, $(BIN_SRC))
# 于是 BIN_OBJ 代表 ./usr/bin 中的那些文件名，加上后缀.o 改到相对位置为 ./build/obj/usr/bin 中的那些文件名
BIN_TARGET := $(patsubst $(BIN_OBJ_DIR)/%.yu.o, $(BIN_TARGET_DIR)/%, $(BIN_OBJ))
# 通过替换 BIN_OBJ，现在 BIN_TARGET 代表 ./build/usr 中的那些文件
USER_IMG := $(BUILD_DIR)/user.img

# compiler flags
YUCFLAGS := -I $(USR_DIR)
CFLAGS := -I$(USR_DIR)


.PHONY: all clean libgrt user

all: libgrt user

clean:
	-rm $(LIB_TARGET)
	-rm -rf $(BIN_TARGET_DIR)
	-rm $(USER_IMG)

libgrt: $(LIB_TARGET)

user: $(USER_IMG)

$(BIN_TARGET_DIR):
	mkdir $@

$(LIB_TARGET): $(LIB_OBJ)
# $(LIB_OBJ) 将根据文件调用 rules.mk 中的函数
	$(info making GeeOS user library...)
	$(AR) $@ $^
	$(RANLIB) $@

$(BIN_TARGET_DIR)/%: $(BIN_OBJ_DIR)/%.yu.o $(LIB_TARGET)
	$(info making user binary "$(notdir $@)"...)
	$(LD) -L$(BUILD_DIR) -lgrt -o $@ $<
	$(OBJD) $@ > $@.dump
	$(if $(filter 0, $(DEBUG)), $(STRIP) $@ -o $@)

$(USER_IMG): $(BIN_TARGET_DIR) $(BIN_TARGET)
# $(BIN_TARGET) 将调用上面的 $(BIN_TARGET_DIR)/%
	$(info making filesystem image...)
	$(BUILD_DIR)/mkfs $@ -c 256 1 2 -a $(BIN_TARGET)

include $(TOP_DIR)/rules.mk
