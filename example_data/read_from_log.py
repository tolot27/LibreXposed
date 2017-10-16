with open('/tmp/libre_data_file') as f:
    content = f.readlines()
# you may also want to remove whitespace characters like `\n` at the end of each line
content = [x.strip() for x in content] 
all_bytes = []
for line in content:
    print(line[11:-17])
    bytes = line[11:-17].split()
    print(bytes)
    all_bytes.extend(bytes)
    
all_bytes = [int(byte, 16) for byte in all_bytes]
print (all_bytes)

newFile = open("filename.bin", "wb")
newFileByteArray = bytearray(all_bytes)
newFile.write(newFileByteArray)    
