# GeeOS

GeeOS (寂) is a lightweight, UNIX like operating system, written in [YuLang](https://github.com/MaxXSoft/YuLang), developed for [Fuxi](https://github.com/MaxXSoft/Fuxi) processor.

## installing dependencies

Before building GeeOS, please make sure you have installed the following dependencies:

* [YuLang](https://github.com/MaxXSoft/YuLang) compiler
* `llvm` and [RISC_V gcc toolchain](http://github.com/riscv/riscv-gnu-toolchain) 
* C++ compiler supporting C++17
* `qemu` >= 4.0.0 but < 5.0.0
* [LLD](https://lld.llvm.org) - The LLVM Linker

### Some Tips

If you have already checked out LLVM using SVN, you can checkout LLD just like you did for clang. 

Or you can build LLD and Clang form a git mirror and build it just like:

```
git clone -b release/10.x https://github.com/llvm/llvm-project llvm-project --depth 1
mkdir build
cd build
cmake -DCMAKE_BUILD_TYPE=Release -DLLVM_ENABLE_PROJECTS="clang;lld" -DLLVM_BUILD_LLVM_DYLIB=ON -DCMAKE_INSTALL_PREFIX=/usr ../llvm-project/llvm
sudo make install
```

`riscv-gnu-toolchain` is needed to cross-compile for a riscv32 target. it is sufficent to do the following for GeeOS: 

```
sudo apt-get install autoconf automake autotools-dev curl libmpc-dev libmpfr-dev libgmp-dev gawk build-essential bison flex texinfo gperf libtool patchutils bc zlib1g-dev libexpat-dev
git clone --recursive https://github.com/riscv/riscv-gnu-toolchain
cd riscv-gnu-toolchain
./configure --prefix=/opt/riscv --with-arch=rv32ima --with-abi=ilp32
make -j9
```

The mirror of riscv-gnu-toolchain can be easily found on Gitee. Or you can run `clone_riscv_toolchain.sh` directly. 
And you need to add `/opt/riscv/bin` to your `PATH` .

## Getting Started

You may want to check the toolchain configuration in `toolchain.mk`. Then you can build this repository by executing the following command lines:

```
$ git clone https://github.com/MaxXSoft/GeeOS.git
$ cd GeeOS
$ make -j8
```

ELF file of GeeOS will be generated in directory `build`. By default, you can run it with QEMU:

```
$ qemu-system-riscv32 -nographic -machine virt -m 128m -kernel build/geeos.elf
```

## Details

> UNDER CONSTRUCTION...

## Changelog

See [CHANGELOG.md](CHANGELOG.md)

## References

GeeOS is heavily influenced by [rCore](https://github.com/rcore-os/rCore) and [xv6](https://github.com/mit-pdos/xv6-riscv).

## License

Copyright (C) 2010-2020 MaxXing. License GPLv3.
