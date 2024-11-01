until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 1
done

# Wait until we are in the launcher
while true; do
    current_focus=$(dumpsys window | grep -E "mCurrentFocus")
    if echo "$current_focus" | grep -q -E "launcher|lawnchair"; then
        echo "vbmeta-fixer: service.sh - launcher started" >> /dev/kmsg
        break
    else
        sleep 1
        echo "vbmeta-fixer: service.sh - waiting for launcher to start" >> /dev/kmsg
    fi
done

# delay for 5 seconds
sleep 5

# Run the service
am start-foreground-service -n com.reveny.vbmetafix.service/.FixerService

echo "vbmeta-fixer: service.sh - service started" >> /dev/kmsg

# Define the boot hash file path
BOOT_HASH_FILE="/data/data/com.reveny.vbmetafix.service/cache/boot.hash"
timeout=5
counter=0

# Attempt to read the boot hash file until it's available or timeout is reached
while [ $counter -lt $timeout ]; do
    if [ -f "$BOOT_HASH_FILE" ]; then
        boot_hash=$(cat "$BOOT_HASH_FILE")
        resetprop ro.boot.vbmeta.digest $boot_hash   
        
        echo "description=Reset the VBMeta digest property with the correct boot hash to fix detection. \nStatus: Service Active ✅" >> $MODPATH/module.prop
        echo "vbmeta-fixer: service.sh - service active" >> /dev/kmsg
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