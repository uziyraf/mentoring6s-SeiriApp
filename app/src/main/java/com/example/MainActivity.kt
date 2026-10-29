package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SeiriRepository
import com.example.model.MentoringReportRepository
import com.example.model.UserRole
import com.example.ui.screens.*
import com.example.ui.screens.MentoringReport.MentoringReportScreen
import com.example.ui.screens.auth.LoginScreen
import com.example.ui.screens.auth.RegisterScreen
import com.example.ui.screens.auth.WelcomeScreen
import com.example.ui.screens.auth.GuestAreaBrowseScreen
import com.example.ui.theme.IndustrialNavy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.PrimaryBlueContainer
import com.example.ui.theme.PrimaryBlueDark
import com.example.ui.theme.SecondarySlate
import com.example.ui.theme.SecondarySlateLight
import com.example.ui.theme.SeiriTheme
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusGreen
import kotlinx.coroutines.launch
import com.example.data.repository.AuthRepository
import androidx.compose.ui.text.style.TextAlign




sealed class Screen(val title: String, val icon: ImageVector) {
    object Dashboard : Screen("Dashboard", Icons.Default.Home)
    object ObjectMaster : Screen("List Data", Icons.Default.Inventory2)
    object AreasAndZones : Screen("Area & Zona", Icons.Default.Apartment)
    object MentorData : Screen("Data Mentor", Icons.Default.People)
    object SpreadsheetImport : Screen("Impor Spreadsheet", Icons.Default.UploadFile)
    object TransferRequests : Screen("Permintaan Perpindahan", Icons.Default.SwapHoriz)
    object MentoringSessions : Screen("Histori Mentoring", Icons.Default.History)
    object MentoringWizard : Screen("Input Sesi Mentoring", Icons.Default.PostAdd)
    object Reports : Screen("Laporan 6S", Icons.Default.BarChart)
    object MentoringReport : Screen("Laporan Mentoring", Icons.Default.Assignment)
    object AuditLog : Screen("Audit Log", Icons.Default.HistoryEdu)
    object QuestionBank : Screen("Bank Pertanyaan", Icons.Default.Quiz)
    object GuestAreas : Screen("Area", Icons.Default.Apartment)
    object MentorProfile : Screen("Akun", Icons.Default.AccountCircle)
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Inisialisasi dipanggil di sini (SEBELUM setContent)
        AuthRepository.initialize(applicationContext)
        
        enableEdgeToEdge()
        setContent {
            SeiriTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // State untuk navigasi Auth
                    var authScreen by remember { mutableStateOf("WELCOME") }
                    val authUser by AuthRepository.currentUser.collectAsState()
                    val isInitializing by AuthRepository.isInitializing.collectAsState()
                    var syncedUserId by remember { mutableStateOf<String?>(null) }

                    // 2. Sinkronisasi user login ke SeiriRepository
                    LaunchedEffect(authUser?.id) {
                        authUser?.let {
                            SeiriRepository.instance.setCurrentUser(it)
                            syncedUserId = it.id
                        } ?: run {
                            syncedUserId = null
                        }
                    }

                    // 3. Routing UI
                    if (isInitializing) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Memuat sesi...", color = SecondarySlate)
                        }
                    } else if (authUser == null) {
                        when (authScreen) {
                            "WELCOME" -> WelcomeScreen(
                                onNavigateToLogin = { authScreen = "LOGIN" },
                                onNavigateToRegister = { authScreen = "REGISTER" },
                                onLoginAsGuest = { /* Dibiarkan kosong karena sudah dihandle WelcomeScreen */ }
                            )
                            "LOGIN" -> LoginScreen(
                                onLoginSuccess = { /* StateFlow akan otomatis mengubah UI */ },
                                onNavigateToRegister = { authScreen = "REGISTER" }
                            )
                            "REGISTER" -> RegisterScreen(
                                onNavigateToLogin = { authScreen = "LOGIN" }
                            )
                        }
                    } else if (syncedUserId != authUser?.id) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "Menyiapkan akun...", color = SecondarySlate)
                        }
                    } else {
                        SeiriApp()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeiriApp() {
    val repository = remember { SeiriRepository.instance }
    val currentUser by repository.currentUser.collectAsState()

    if (currentUser.role == UserRole.MENTOR && currentUser.assignedAreaId == null) {
        MentorPendingAssignmentScreen(repository)
        return
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // =========================================================================
    // TAMBAHAN BARU: Sinkronisasi Laporan Mentoring dari Firebase saat aplikasi dibuka
    // =========================================================================
    LaunchedEffect(Unit) {
        MentoringReportRepository.initSync()
    }

    val defaultScreen = when (currentUser.role) {
        UserRole.GUEST -> Screen.GuestAreas
        else -> Screen.Dashboard
    }
    var currentScreen by remember { mutableStateOf<Screen>(defaultScreen) }
    var preselectedTransferObjectId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(currentUser.role) {
        currentScreen = defaultScreen
    }

    // Predictive Back Handling
    BackHandler(enabled = drawerState.isOpen || (currentUser.role != UserRole.GUEST && currentScreen != Screen.Dashboard)) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            currentScreen = Screen.Dashboard
        }
    }

    val adminNavItems = listOf(
        Screen.Dashboard,
        Screen.ObjectMaster,
        Screen.AreasAndZones,
        Screen.MentorData,
        Screen.SpreadsheetImport,
        Screen.TransferRequests,
        Screen.MentoringSessions,
        Screen.MentoringReport,
        Screen.Reports,
        Screen.AuditLog,
        Screen.QuestionBank
    )

    val mentorNavItems = listOf(
        Screen.Dashboard,
        Screen.MentoringWizard,
        Screen.MentoringSessions,
        Screen.ObjectMaster,
        Screen.MentorProfile,
        Screen.TransferRequests,
        Screen.MentoringReport,
        Screen.Reports
    )

    val guestNavItems = listOf(
        Screen.GuestAreas
    )

    val effectiveMentorNavItems = if (currentUser.role == UserRole.MENTOR && currentUser.assignedAreaId == null) {
        mentorNavItems.filter { it != Screen.MentoringWizard }
    } else {
        mentorNavItems
    }
    val currentNavItems = when (currentUser.role) {
        UserRole.ADMIN -> adminNavItems
        UserRole.MENTOR -> effectiveMentorNavItems
        UserRole.GUEST -> guestNavItems
    }

    LaunchedEffect(currentScreen, currentNavItems) {
        if (!currentNavItems.contains(currentScreen)) {
            currentScreen = defaultScreen
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                // Drawer Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IndustrialNavy)
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (currentUser.role == UserRole.ADMIN) PrimaryBlue else StatusGreen),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = currentUser.name.take(2).uppercase(),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = currentUser.name,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentUser.role.label,
                                color = SecondarySlateLight,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    if (currentUser.assignedAreaId != null) {
                        Surface(
                            color = Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Binaan: ${repository.getAreaName(currentUser.assignedAreaId ?: "")}",
                                color = Color.White,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Navigation Items
                currentNavItems.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        selected = isSelected,
                        onClick = {
                            currentScreen = screen
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Drawer Footer: Akun dan Keluar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .clickable {
                            scope.launch { drawerState.close() }
                            AuthRepository.logout()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Keluar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(currentUser.email, fontSize = 10.sp, color = SecondarySlate)
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = if (currentUser.role == UserRole.MENTOR && currentScreen == Screen.Dashboard) "Home" else currentScreen.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Seiri Mentoring 6S • ${currentUser.name} (${currentUser.role.label})",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                color = SecondarySlate
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("nav_drawer_toggle")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Buka Menu")
                        }
                    },
                    actions = {
                        Text(
                            text = currentUser.email,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = SecondarySlate,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            bottomBar = { }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentScreen) {
                    Screen.Dashboard -> DashboardScreen(
                        repository = repository,
                        onNavigateToMaster = { currentScreen = Screen.ObjectMaster },
                        onNavigateToImport = { currentScreen = Screen.SpreadsheetImport },
                        onNavigateToTransfers = {
                            preselectedTransferObjectId = null
                            currentScreen = Screen.TransferRequests
                        },
                        onNavigateToWizard = { currentScreen = Screen.MentoringWizard },
                        onNavigateToSessions = { currentScreen = Screen.MentoringSessions },
                        onNavigateToReports = { currentScreen = Screen.Reports }
                    )

                    Screen.ObjectMaster -> ObjectMasterScreen(
                        repository = repository,
                        onNavigateToTransfers = { objectId ->
                            preselectedTransferObjectId = objectId
                            currentScreen = Screen.TransferRequests
                        }
                    )

                    Screen.AreasAndZones -> AreaZoneScreen(
                        repository = repository
                    )

                    Screen.MentorData -> MentorListScreen(repository = repository)

                    Screen.SpreadsheetImport -> SpreadsheetImportScreen(
                        repository = repository,
                        onNavigateBack = { currentScreen = Screen.ObjectMaster }
                    )

                    Screen.TransferRequests -> TransferRequestsScreen(
                        repository = repository,
                        preselectedObjectId = preselectedTransferObjectId
                    )

                    Screen.MentoringSessions -> SessionsScreen(
                        repository = repository,
                        onStartNewSession = { currentScreen = Screen.MentoringWizard }
                    )

                    Screen.MentoringWizard -> MentoringWizardScreen(
                        repository = repository,
                        onNavigateBack = { currentScreen = Screen.Dashboard },
                        onNavigateToTransfers = { objectId ->
                            preselectedTransferObjectId = objectId
                            currentScreen = Screen.TransferRequests
                        }
                    )

                    Screen.Reports -> ReportsScreen(
                        repository = repository
                    )

                    Screen.MentoringReport -> MentoringReportScreen(
                        repository = repository,
                        sessionId = "",
                        mentorName = currentUser.name,
                        mentorId = currentUser.id
                    )

                    Screen.AuditLog -> AuditLogScreen(
                        repository = repository
                    )

                    Screen.QuestionBank -> QuestionBankScreen(
                        repository = repository
                    )

                    Screen.MentorProfile -> MentorProfileScreen(
                        repository = repository,
                        onLogout = { AuthRepository.logout() },
                        onStartWizard = { currentScreen = Screen.MentoringWizard }
                    )

                    Screen.GuestAreas -> GuestAreaBrowseScreen()
                }
            }
        }
    }
}

@Composable
fun MentorProfileScreen(
    repository: SeiriRepository,
    onLogout: () -> Unit,
    onStartWizard: () -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val objects by repository.objects.collectAsState()
    val sessions by repository.sessions.collectAsState()
    val validations by repository.validations.collectAsState()

    val myObjects = objects.filter { it.areaId == currentUser.assignedAreaId && it.isActive }
    val mySessions = sessions.filter { it.mentorId == currentUser.id }
    val myValidations = validations.filter { it.createdBy == currentUser.name }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(PrimaryBlue),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentUser.name.take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = currentUser.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${currentUser.role.label} • ${currentUser.department}",
                        fontSize = 12.sp,
                        color = SecondarySlate
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = PrimaryBlueContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Area Binaan: ${repository.getAreaName(currentUser.assignedAreaId ?: "")}",
                            color = PrimaryBlueDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Statistik Mentoring Saya", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Objek di Area:", fontSize = 12.sp, color = SecondarySlate)
                        Text("${myObjects.size} Objek", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sesi Mentoring Dilaksanakan:", fontSize = 12.sp, color = SecondarySlate)
                        Text("${mySessions.size} Sesi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Validasi Dibuat:", fontSize = 12.sp, color = SecondarySlate)
                        Text("${myValidations.size} Rekor", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusGreen)
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onStartWizard,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mulai Sesi Mentoring Baru", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onLogout,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Keluar")
                }
            }
        }
    }
}

@Composable
fun MentorPendingAssignmentScreen(repository: SeiriRepository) {
    val currentUser by repository.currentUser.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = StatusAmber,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Akun Menunggu Penugasan Area",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = IndustrialNavy
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Halo, ${currentUser.name}. Akun Anda sudah aktif sebagai Mentor 6S, tetapi area binaan belum ditentukan oleh admin.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SecondarySlate,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Silakan hubungi admin IT untuk menetapkan area binaan. Setelah area ditetapkan, Anda bisa mulai sesi mentoring.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SecondarySlateLight,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                OutlinedButton(
                    onClick = { AuthRepository.logout() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlueDark)
                ) {
                    Text("Keluar", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}