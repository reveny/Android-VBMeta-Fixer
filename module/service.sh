until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 1
done

# Define paths
BOOT_HASH_FILE="/data/data/com.reveny.vbmetafix.service/cache/boot.hash"
TARGET="/data/adb/tricky_store/target.txt"
timeout=10
counter=0

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

# Delay for 10 seconds which is hopefully enough
sleep 10

# Remove old to prevent getting applied if generation failed.
rm -rf $BOOT_HASH_FILE

# Add to target.txt if not already present
if ! grep -q "com.reveny.vbmetafix.service" "$TARGET"; then
    sed -i -e ':a' -e '/^\n*$/{$d;N;};/\n$/ba' "$TARGET"
    echo "com.reveny.vbmetafix.service" >> "$TARGET"
fi

# Run the service
am start-foreground-service -n com.reveny.vbmetafix.service/.FixerService --user 0 </dev/null 1>/dev/null 2>&1
echo "vbmeta-fixer: service.sh - service started" >> /dev/kmsg

# Attempt to read the boot hash file until it's available or timeout is reached
while [ $counter -lt $timeout ]; do
    if [ -f "$BOOT_HASH_FILE" ]; then
        boot_hash=$(cat "$BOOT_HASH_FILE")
        if [ "$boot_hash" == "null" ]; then
            boot_hash=""
        fi
        resetprop ro.boot.vbmeta.digest "$boot_hash"
        resetprop ro.boot.vbmeta.hash_alg "sha256"

        vbmeta_size=$(/bin/toybox blockdev --getbs $(echo -n "/dev/block/by-name/vbmeta"$(getprop ro.boot.slot_suffix)))
        resetprop ro.boot.vbmeta.size "$vbmeta_size"

        resetprop ro.boot.vbmeta.invalidate_on_error "yes"
        resetprop ro.boot.vbmeta.device_state "locked"
        
        echo "description=Reset the VBMeta digest property with the correct boot hash to fix detection.\nStatus: Service Active ✅" >> "$MODPATH/module.prop"
        echo "vbmeta-fixer: service.sh - service active" >> /dev/kmsg
        break
    else
        sleep 1
        if [ -d "/data/data/com.reveny.vbmetafix.service/cache" ]; then
        am start-foreground-service -n com.reveny.vbmetafix.service/.FixerService --user 0 </dev/null 1>/dev/null 2>&1
        fi
        counter=$((counter + 1))
    fi
done

# Print fail message if the boot hash file was not read within 5 seconds
if [ $counter -ge $timeout ]; then
    echo "description=Reset the VBMeta digest property with the correct boot hash to fix detection.\nStatus: Failed ❌" >> "$MODPATH/module.prop"
    echo "vbmeta-fixer: service.sh - failed to reset VBMeta digest within timeout" >> /dev/kmsg
fi
