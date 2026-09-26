# AirPlayr native Android application.
# Project-specific shrinker rules can be added here when needed.

# jaudiotagger contains optional desktop-Java artwork image APIs.
# These APIs are not available on Android and are not used by AirPlayr.
-dontwarn java.awt.image.BufferedImage
-dontwarn javax.imageio.ImageIO
-dontwarn javax.imageio.stream.ImageInputStream
