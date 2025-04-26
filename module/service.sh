#!/system/bin/sh

MODDIR="/data/adb/modules/vbmeta-fixer"

# Update module status
update_status() {
    local status_text="$1"
    local status_emoji="$2"
    local new_description="description=Reset the VBMeta digest property with the correct boot hash to fix detection. Status: $status_text $status_emoji"

    sed -i "s|^description=.*|$new_description|" "$MODPATH/module.prop"

    echo "vbmeta-fixer: service.sh - $status_text" >> /dev/kmsg
}

BOOT_HASH_FILE="/data/data/com.reveny.vbmetafix.service/cache/boot.hash"
TARGET="/data/adb/tricky_store/target.txt"
timeout=10
counter=0
launcher_timeout=20
launcher_counter=0

update_status "Initializing" "⏳"

echo "vbmeta-fixer: service.sh - waiting for boot completion" >> /dev/kmsg
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 1
done
echo "vbmeta-fixer: service.sh - boot completed" >> /dev/kmsg
update_status "Boot completed, waiting for launcher" "⏳"

# Wait until we are in the launcher
while true; do
    if dumpsys activity activities | grep -q "mResumedActivity" | grep -q -E "launcher|Launcher|lawnchair"; then
        echo "vbmeta-fixer: service.sh - launcher detected via activities" >> /dev/kmsg
        update_status "Launcher detected (via activities)" "⏳"
        break
    fi
    
    if dumpsys activity recents | grep -q "Recent #0" | grep -q -E "launcher|Launcher|lawnchair"; then
        echo "vbmeta-fixer: service.sh - launcher detected via recents" >> /dev/kmsg
        update_status "Launcher detected (via recents)" "⏳"
        break
    fi
    
    launcher_counter=$((launcher_counter + 1))
    if [ $launcher_counter -gt $launcher_timeout ]; then
        echo "vbmeta-fixer: service.sh - launcher detection timed out after ${launcher_timeout}s, continuing anyway" >> /dev/kmsg
        update_status "Launcher timeout, continuing" "⚠️"
        break
    fi
    
    sleep 1
    echo "vbmeta-fixer: service.sh - waiting for launcher to start (${launcher_counter}/${launcher_timeout}s)" >> /dev/kmsg
done

update_status "Launcher ready, stabilizing system" "⏳"
sleep 10

rm -rf $BOOT_HASH_FILE
update_status "Starting service" "⏳"

# Add to target.txt if not already present
if [ -f "$TARGET" ]; then
    if ! grep -q "com.reveny.vbmetafix.service" "$TARGET"; then
        sed -i -e ':a' -e '/^\n*$/{$d;N;};/\n$/ba' "$TARGET"
        echo "com.reveny.vbmetafix.service" >> "$TARGET"
        echo "vbmeta-fixer: service.sh - added to target.txt" >> /dev/kmsg
    else
        echo "vbmeta-fixer: service.sh - already in target.txt" >> /dev/kmsg
    fi
else
    mkdir -p "$(dirname "$TARGET")"
    echo "com.reveny.vbmetafix.service" > "$TARGET"
    echo "vbmeta-fixer: service.sh - created target.txt" >> /dev/kmsg
fi

am start-foreground-service -n com.reveny.vbmetafix.service/.FixerService --user 0 </dev/null 1>/dev/null 2>&1
echo "vbmeta-fixer: service.sh - service started" >> /dev/kmsg
update_status "Service started, waiting for hash file" "⏳"

while [ $counter -lt $timeout ]; do
    if [ -f "$BOOT_HASH_FILE" ]; then
        boot_hash=$(cat "$BOOT_HASH_FILE")
        if [ "$boot_hash" == "null" ]; then
            boot_hash=""
            echo "vbmeta-fixer: service.sh - hash file contains null, using empty string" >> /dev/kmsg
        else
            echo "vbmeta-fixer: service.sh - hash file loaded successfully" >> /dev/kmsg
        fi
        
        update_status "Setting VBMeta properties" "⏳"
        
        # Set all VBMeta properties
        resetprop ro.boot.vbmeta.digest "$boot_hash"
        resetprop ro.boot.vbmeta.hash_alg "sha256"
        resetprop ro.boot.vbmeta.avb_version 1.0

        vbmeta_size=$(/bin/toybox blockdev --getbs $(echo -n "/dev/block/by-name/vbmeta"$(getprop ro.boot.slot_suffix)))
        resetprop ro.boot.vbmeta.size "$vbmeta_size"

        resetprop ro.boot.vbmeta.invalidate_on_error "yes"
        resetprop ro.boot.vbmeta.device_state "locked"
        
        update_status "Service Active" "✅"
        echo "vbmeta-fixer: service.sh - service active and properties set" >> /dev/kmsg
        break
    else
        sleep 1
        if [ -d "/data/data/com.reveny.vbmetafix.service/cache" ]; then
            echo "vbmeta-fixer: service.sh - cache directory exists, restarting service" >> /dev/kmsg
            am start-foreground-service -n com.reveny.vbmetafix.service/.FixerService --user 0 </dev/null 1>/dev/null 2>&1
        else
            echo "vbmeta-fixer: service.sh - waiting for cache directory to be created ($counter/$timeout)" >> /dev/kmsg
        fi
        counter=$((counter + 1))
    fi
done

# Check if we timed out
if [ $counter -ge $timeout ]; then
    update_status "Failed to set VBMeta properties" "❌"
    echo "vbmeta-fixer: service.sh - failed to reset VBMeta digest within timeout" >> /dev/kmsg
fi

echo "vbmeta-fixer: service.sh - script completed" >> /dev/kmsg