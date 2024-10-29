#!/usr/bin/env sh

# Wait until the system boot is completed
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 1
done

sleep 10

# Run the service
am start-foreground-service -n com.reveny.vbmetafix.service/.FixerService

# Define the boot hash file path
BOOT_HASH_FILE="/data/data/com.reveny.vbmetafix.service/cache/boot.hash"
timeout=5
counter=0

# Attempt to read the boot hash file until it's available or timeout is reached
while [ $counter -lt $timeout ]; do
    if [ -f "$BOOT_HASH_FILE" ]; then
        boot_hash=$(cat "$BOOT_HASH_FILE")
        resetprop ro.boot.vbmeta.digest $boot_hash
        break
    else
        sleep 1
        counter=$((counter + 1))
    fi
done

# Print fail message if the boot hash file was not read within 5 seconds
if [ $counter -ge $timeout ]; then
    echo "description=Reset the VBMeta digest property with the correct boot hash to fix detection. \nStatus: Failed ❌" >> $MODPATH/module.prop
fi