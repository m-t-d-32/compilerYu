cd ~
echo "riscv-gnu-toolchain will be downloaded to ${PWD}"

sudo apt-get install autoconf automake autotools-dev curl libmpc-dev libmpfr-dev libgmp-dev gawk build-essential bison

git clone https://github.com/riscv/riscv-gnu-toolchain
cd riscv-gnu-toolchain

git clone https://gitee.com/mirrors/qemu.git --depth 1
git clone https://gitee.com/mirrors/riscv-binutils-gdb.git --depth 1
rm riscv-binutils -rf
rm riscv-gdb -rf
cp riscv-binutils-gdb riscv-binutils -rf
cp riscv-binutils-gdb riscv-gdb -rf

git clone https://gitee.com/mirrors/riscv-dejagnu.git --depth 1
git clone https://gitee.com/mirrors/riscv-gcc.git --depth 1

git clone git://sourceware.org/git/glibc.git --depth 1
rm riscv-glibc -rf
mv glibc/ riscv-glibc/

git clone git://sourceware.org/git/newlib-cygwin.git --depth 1
rm riscv-newlib -rf
mv newlib-cygwin/ riscv-newlib/

echo "riscv-gnu-toolchain has been downloaded!"

./configure --prefix=/opt/riscv --with-arch=rv32ima --with-abi=ilp32
make -j9

echo "==================================="
echo "Now you need to add /opt/riscv/bin to your PATH!"
echo "==================================="
