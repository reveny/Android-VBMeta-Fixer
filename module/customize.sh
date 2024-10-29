ABI="$(getprop ro.product.cpu.abi)"
if [ "$API" -lt 26 ]; then
    abort "You can't use this module on Android below 8.0"
fi

ui_print "- Module Path: $MODPATH"

# Check if the package is installed
if pm list packages | grep -q "com.reveny.vbmetafix.service"; then
    ui_print "- Previous version found. Uninstalling..."
    uninstall_output=$(pm uninstall com.reveny.vbmetafix.service)
    ui_print "- $uninstall_output"
    ui_print "- Uninstall done!"
else
    ui_print "- No previous version found."
fi

# Install service.apk and print output
ui_print "- Installing service..."
install_output=$(pm install "$MODPATH/service/service.apk")
ui_print "- $install_output"

# Verify if the package is installed after installation attempt
if pm list packages | grep -q "com.reveny.vbmetafix.service"; then
    ui_print "- Install successful!"
    # Set status to active
    sed -i '/description/d' $MODPATH/module.prop

    if [ -w /system/etc/hosts ]; then
        echo "description=Reset the VBMeta digest property with the correct boot hash to fix detection. \nStatus: Active ✅" >> $MODPATH/module.prop
    else
        echo "description=Reset the VBMeta digest property with the correct boot hash to fix detection. \nStatus: Failed ❌" >> $MODPATH/module.prop
    fi
else
    ui_print "- Install failed. Package not found after installation attempt."
	echo "description=Reset the VBMeta digest property with the correct boot hash to fix detection. \nStatus: Failed ❌" >> $MODPATH/module.prop
fi