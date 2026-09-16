# Room generates implementations reflectively referenced at runtime.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-dontwarn androidx.room.paging.**

# ViewModels are instantiated reflectively by the ViewModel factory
# (viewModel()/AndroidViewModelFactory calls the constructor by reflection),
# so keep their constructors under R8 full mode.
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }
-keep class * extends androidx.lifecycle.AndroidViewModel { <init>(...); }

# CameraX and ML Kit ship their own consumer ProGuard rules; nothing extra
# needed here. Add app-specific keeps below if a future feature uses reflection.
