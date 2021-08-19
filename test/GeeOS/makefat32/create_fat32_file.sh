#请用sudo运行这个脚本
rm -rf user_fat32.img
rm -rf disk
dd if=/dev/zero of=user_fat32.img bs=512 count=10000 #创建
mkfs.vfat -F 32 user_fat32.img #格式化
mkdir disk 
mount user_fat32.img disk -o loop -t vfat #挂载
cp alloc disk
cp notepad disk
cp shell disk
cp hello disk
cp helloabcdefghijklmn disk
umount user_fat32.img #卸载
chmod 777 user_fat32.img
