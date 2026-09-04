package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Co2
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import com.example.ui.components.OnboardingCarouselDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.bluetooth.BluetoothState
import com.example.ui.EcoMindViewModel
import com.example.ui.screens.AiGuideScreen
import com.example.ui.screens.CatalogScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.HardwareSetupScreen
import com.example.ui.screens.RfidManagementScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.EcoBadgeGood
import com.example.ui.theme.EcoMindTheme
import com.example.util.EcoMindFeature
import com.example.util.EcoMindIconMapper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  private val viewModel: EcoMindViewModel by viewModels()
  private val bleScanningViewModel: com.example.ui.BleScanningViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      EcoMindTheme {
        EcoMindApp(viewModel = viewModel, bleScanningViewModel = bleScanningViewModel)
      }
    }
  }

  override fun onResume() {
    super.onResume()
    viewModel.nfcRfidScannerManager.enableReaderMode(this)
  }

  override fun onPause() {
    super.onPause()
    viewModel.nfcRfidScannerManager.disableReaderMode(this)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoMindApp(
  viewModel: EcoMindViewModel,
  bleScanningViewModel: com.example.ui.BleScanningViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val prefs = remember { context.getSharedPreferences("ecomind_app_prefs", android.content.Context.MODE_PRIVATE) }
  var showOnboarding by remember { mutableStateOf(false) }

  val bluetoothState by viewModel.bluetoothState.collectAsState()
  val currentUser by viewModel.currentUser.collectAsState()
  val isFirestoreSyncing by viewModel.isFirestoreSyncing.collectAsState()
  val isFirestoreOfflineMode by viewModel.isFirestoreOfflineMode.collectAsState()
  val pendingOfflineWritesCount by viewModel.pendingOfflineWritesCount.collectAsState()
  var showAuthDialog by remember { mutableStateOf(false) }

  // Dynamic Runtime Permission Request Flow for Bluetooth & Location
  val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    viewModel.bluetoothManager.autoConnectLastDevice()
  }

  LaunchedEffect(Unit) {
    com.example.fcm.EcoMindMessagingService.createNotificationChannel(context)

    val neededPermissions = mutableListOf<String>()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
        neededPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
        neededPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
      }
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
        neededPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
      }
    }
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      neededPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      neededPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    if (neededPermissions.isNotEmpty()) {
      permissionLauncher.launch(neededPermissions.toTypedArray())
    } else {
      // If permissions are already granted, attempt safe auto-reconnection to last saved HC-05 device
      viewModel.bluetoothManager.autoConnectLastDevice()
    }
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        modifier = Modifier.width(320.dp)
      ) {
        Column(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
          // --- HEADER SECTION ---
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .weight(1f)
                .clickable {
                  selectedTab = 5
                  scope.launch { drawerState.close() }
                }
            ) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(CircleShape)
                  .background(if (currentUser != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
              ) {
                if (currentUser != null) {
                  Text(
                    text = (currentUser?.displayName?.take(1) ?: currentUser?.email?.take(1) ?: "U").uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                  )
                } else {
                  Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "User Avatar",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                  )
                }
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = currentUser?.displayName ?: "Eco Sentinel",
                  fontSize = 15.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1
                )
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = if (currentUser != null) Color(0xFF2E7D32).copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                  modifier = Modifier.padding(top = 2.dp)
                ) {
                  Text(
                    text = if (currentUser != null) (currentUser?.email ?: "jkvlogs2204@gmail.com") else "Tap to Sign In",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (currentUser != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
            }

            IconButton(
              onClick = { scope.launch { drawerState.close() } },
              modifier = Modifier
                .size(36.dp)
                .testTag("btn_drawer_close")
            ) {
              Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close Navigation Drawer",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          Spacer(modifier = Modifier.height(16.dp))

          // --- SECTION 1: CORE NAVIGATION ---
          Text(
            text = "CORE NAVIGATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
          )

          NavigationDrawerItem(
            label = { Text("Home / Dashboard", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
            selected = selectedTab == 0,
            onClick = {
              selectedTab = 0
              scope.launch { drawerState.close() }
            },
            icon = {
              Icon(
                imageVector = EcoMindIconMapper.getIcon(EcoMindFeature.DASHBOARD, selectedTab == 0),
                contentDescription = "Dashboard"
              )
            },
            colors = NavigationDrawerItemDefaults.colors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .padding(vertical = 2.dp)
              .testTag("drawer_item_dashboard")
          )

          NavigationDrawerItem(
            label = { Text("Carbon & Eco Footprint", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
            selected = false,
            onClick = {
              selectedTab = 0
              scope.launch { drawerState.close() }
            },
            icon = {
              Icon(
                imageVector = EcoMindIconMapper.getIcon(EcoMindFeature.CARBON_TRACKER, false),
                contentDescription = "Carbon Tracker"
              )
            },
            colors = NavigationDrawerItemDefaults.colors(
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .padding(vertical = 2.dp)
              .testTag("drawer_item_co2")
          )

          NavigationDrawerItem(
            label = { Text("RFID & Sensor Zones", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
            selected = selectedTab == 2,
            onClick = {
              selectedTab = 2
              scope.launch { drawerState.close() }
            },
            icon = {
              Icon(
                imageVector = EcoMindIconMapper.getIcon(EcoMindFeature.RFID_ZONES, selectedTab == 2),
                contentDescription = "RFID Zones"
              )
            },
            colors = NavigationDrawerItemDefaults.colors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .padding(vertical = 2.dp)
              .testTag("drawer_item_rfid")
          )

          NavigationDrawerItem(
            label = { Text("Eco Product Catalog", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
            selected = selectedTab == 3,
            onClick = {
              selectedTab = 3
              scope.launch { drawerState.close() }
            },
            icon = {
              Icon(
                imageVector = EcoMindIconMapper.getIcon(EcoMindFeature.CATALOG, selectedTab == 3),
                contentDescription = "Catalog"
              )
            },
            colors = NavigationDrawerItemDefaults.colors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .padding(vertical = 2.dp)
              .testTag("drawer_item_catalog")
          )

          Spacer(modifier = Modifier.height(12.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
          Spacer(modifier = Modifier.height(12.dp))

          // --- SECTION 2: HARDWARE & IOT NODES ---
          Text(
            text = "HARDWARE & IOT NODES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
          )

          NavigationDrawerItem(
            label = { Text("BLE Hardware Setup", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
            selected = selectedTab == 1,
            onClick = {
              selectedTab = 1
              scope.launch { drawerState.close() }
            },
            icon = {
              Icon(
                imageVector = EcoMindIconMapper.getIcon(EcoMindFeature.HARDWARE, selectedTab == 1),
                contentDescription = "Hardware Nodes"
              )
            },
            colors = NavigationDrawerItemDefaults.colors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .padding(vertical = 2.dp)
              .testTag("drawer_item_hardware")
          )

          NavigationDrawerItem(
            label = { Text("AI Eco Assistant", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
            selected = selectedTab == 4,
            onClick = {
              selectedTab = 4
              scope.launch { drawerState.close() }
            },
            icon = {
              Icon(
                imageVector = EcoMindIconMapper.getIcon(EcoMindFeature.AI_GUIDE, selectedTab == 4),
                contentDescription = "AI Eco Guide"
              )
            },
            colors = NavigationDrawerItemDefaults.colors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .padding(vertical = 2.dp)
              .testTag("drawer_item_ai_guide")
          )

          Spacer(modifier = Modifier.height(12.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
          Spacer(modifier = Modifier.height(12.dp))

          // --- SECTION 3: SYSTEM & ACCOUNT ---
          Text(
            text = "SYSTEM & PREFERENCES",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
          )

          NavigationDrawerItem(
            label = { Text("Settings & Calibration", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
            selected = selectedTab == 5,
            onClick = {
              selectedTab = 5
              scope.launch { drawerState.close() }
            },
            icon = {
              Icon(
                imageVector = EcoMindIconMapper.getIcon(EcoMindFeature.SETTINGS, selectedTab == 5),
                contentDescription = "Settings"
              )
            },
            colors = NavigationDrawerItemDefaults.colors(
              selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
              selectedIconColor = MaterialTheme.colorScheme.primary,
              selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
              unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
              unselectedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .padding(vertical = 2.dp)
              .testTag("drawer_item_settings")
          )

          NavigationDrawerItem(
            label = { Text("App Feature Tour", fontSize = 14.sp, fontWeight = FontWeight.SemiBold) },
            selected = false,
            onClick = {
              showOnboarding = true
              scope.launch { drawerState.close() }
            },
            icon = {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "Onboarding Tour"
              )
            },
            colors = NavigationDrawerItemDefaults.colors(
              unselectedIconColor = EcoBadgeGood,
              unselectedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
              .padding(vertical = 2.dp)
              .testTag("drawer_item_onboarding_tour")
          )

          Spacer(modifier = Modifier.weight(1f, fill = false))
          Spacer(modifier = Modifier.height(24.dp))

          // --- FOOTER SECTION: QUICK ECO-TIP & SYSTEM STATUS ---
          Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.Lightbulb,
                  contentDescription = "Eco Tip",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "Quick Eco Tip",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary
                )
              }
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Maintaining indoor CO2 < 800 ppm increases cognitive focus by up to 15%. Keep ventilation active!",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }
      }
    }
  ) {
    Scaffold(
      topBar = {
        CenterAlignedTopAppBar(
          navigationIcon = {
            IconButton(
              onClick = { scope.launch { drawerState.open() } },
              modifier = Modifier
                .padding(start = 4.dp)
                .testTag("btn_top_bar_menu")
            ) {
              Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Open Navigation Drawer",
                tint = MaterialTheme.colorScheme.onSurface
              )
            }
          },
          title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(32.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Eco,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(20.dp)
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "Eco Mind",
                  fontSize = 18.sp,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = "IoT Environmental Intelligence",
                  fontSize = 10.sp,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          },
          actions = {
            // Firestore Sync Status Icon Badge
            val syncIcon = when {
              isFirestoreSyncing -> Icons.Default.CloudSync
              isFirestoreOfflineMode -> Icons.Default.CloudOff
              else -> Icons.Default.CloudDone
            }

            val syncColor = when {
              isFirestoreSyncing -> Color(0xFFFF8F00)
              isFirestoreOfflineMode -> Color(0xFFD32F2F)
              else -> Color(0xFF2E7D32)
            }

            val syncLabel = when {
              isFirestoreSyncing -> "SYNCING"
              isFirestoreOfflineMode -> if (pendingOfflineWritesCount > 0) "OFFLINE ($pendingOfflineWritesCount)" else "OFFLINE"
              else -> "ONLINE"
            }

            Surface(
              shape = CircleShape,
              color = syncColor.copy(alpha = 0.15f),
              modifier = Modifier
                .padding(end = 6.dp)
                .testTag("firestore_status_indicator")
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = syncIcon,
                  contentDescription = "Firestore Status $syncLabel",
                  tint = syncColor,
                  modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = syncLabel,
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  color = syncColor
                )
              }
            }

            // Top Bluetooth Status Dot
            Surface(
              shape = CircleShape,
              color = if (bluetoothState is BluetoothState.Connected) EcoBadgeGood.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f),
              modifier = Modifier.padding(end = 8.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(if (bluetoothState is BluetoothState.Connected) EcoBadgeGood else Color.Gray)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                  text = if (bluetoothState is BluetoothState.Connected) "BT LIVE" else "OFFLINE",
                  fontSize = 8.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (bluetoothState is BluetoothState.Connected) EcoBadgeGood else Color.Gray
                )
              }
            }

            IconButton(
              onClick = { selectedTab = 5 },
              modifier = Modifier
                .padding(end = 2.dp)
                .testTag("btn_top_bar_auth")
            ) {
              if (currentUser != null) {
                Box(
                  modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2E7D32)),
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = (currentUser?.displayName?.take(1) ?: currentUser?.email?.take(1) ?: "U").uppercase(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              } else {
                Icon(
                  imageVector = Icons.Default.AccountCircle,
                  contentDescription = "Firebase Sign In",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }

            IconButton(
              onClick = { selectedTab = 5 },
              modifier = Modifier
                .padding(end = 4.dp)
                .testTag("btn_top_bar_settings")
            ) {
              Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = if (selectedTab == 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          },
          colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
          )
        )
      },
      bottomBar = {
        NavigationBar(
          modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
          containerColor = MaterialTheme.colorScheme.surface,
          tonalElevation = 8.dp
        ) {
          NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            icon = { Icon(imageVector = EcoMindIconMapper.getIcon(EcoMindFeature.DASHBOARD, selectedTab == 0), contentDescription = "Dashboard") },
            label = { Text("Dashboard", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_dashboard")
          )
          NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            icon = { Icon(imageVector = EcoMindIconMapper.getIcon(EcoMindFeature.HARDWARE, selectedTab == 1), contentDescription = "Hardware") },
            label = { Text("Hardware", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_hardware")
          )
          NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            icon = { Icon(imageVector = EcoMindIconMapper.getIcon(EcoMindFeature.RFID_ZONES, selectedTab == 2), contentDescription = "RFID Zones") },
            label = { Text("RFID Zones", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_rfid_zones")
          )
          NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { selectedTab = 3 },
            icon = { Icon(imageVector = EcoMindIconMapper.getIcon(EcoMindFeature.CATALOG, selectedTab == 3), contentDescription = "Catalog") },
            label = { Text("Catalog", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_catalog")
          )
          NavigationBarItem(
            selected = selectedTab == 4,
            onClick = { selectedTab = 4 },
            icon = { Icon(imageVector = EcoMindIconMapper.getIcon(EcoMindFeature.AI_GUIDE, selectedTab == 4), contentDescription = "Eco Guide") },
            label = { Text("Eco Guide", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = MaterialTheme.colorScheme.primary,
              indicatorColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.testTag("nav_tab_eco_guide")
          )
        }
      },
      modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        when (selectedTab) {
          0 -> DashboardScreen(
            viewModel = viewModel,
            onNavigateToHardware = { selectedTab = 1 }
          )
          1 -> HardwareSetupScreen(
            viewModel = viewModel,
            bleScanningViewModel = bleScanningViewModel
          )
          2 -> RfidManagementScreen(
            viewModel = viewModel,
            onNavigateToDashboard = { selectedTab = 0 }
          )
          3 -> CatalogScreen(
            viewModel = viewModel,
            onSelectProduct = { selectedTab = 0 }
          )
          4 -> AiGuideScreen(viewModel = viewModel)
          5 -> SettingsScreen(viewModel = viewModel)
        }
      }
    }
  }
}

