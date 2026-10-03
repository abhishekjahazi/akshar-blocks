# Keep rules for release builds (R8 runs in full mode).
#
# The ads SDK brings WorkManager 2.7 and Room 2.2, whose own rules keep these classes but not
# their no-argument constructors, which they create by reflection. Full mode removes such
# constructors, and the app then crashed at start with "Failed to create an instance of
# androidx.work.impl.WorkDatabase".
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.work.InputMerger {
    <init>();
}
