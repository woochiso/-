package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PieChartOutline
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.EmotionDiaryScreen
import com.example.ui.screens.EmotionDiaryHubScreen
import com.example.ui.screens.AiCareComingSoonScreen
import com.example.ui.screens.AiEmotionAnalysisScreen
import com.example.ui.screens.AiCounselingScreen
import com.example.ui.screens.AiCareScreen
import com.example.ui.screens.AiTrainingScreen
import com.example.ui.screens.AiTrainingEntryScreen
import com.example.ui.screens.HumorTrainingScreen
import com.example.ui.screens.VocalTrainingScreen
import com.example.ui.screens.WitTrainingScreen
import com.example.ui.screens.DatingTrainingScreen
import com.example.ui.screens.ExpressionTrainingScreen
import com.example.ui.screens.QuizTrainingScreen
import com.example.ui.screens.DebateTrainingScreen
import com.example.ui.screens.RecoveryScreen
import com.example.ui.screens.FavoriteEmotionsScreen
import com.example.ui.screens.HumanEmotionsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InnerStoriesScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.GoogleSignupScreen
import com.example.ui.screens.SignupScreen
import com.example.ui.screens.ForgotPasswordScreen
import com.example.ui.screens.MyInfoComingSoonScreen
import com.example.ui.screens.MyInfoScreen
import com.example.ui.screens.AppInfoScreen
import com.example.ui.screens.EditProfileScreen
import com.example.ui.screens.ChangePasswordScreen
import com.example.ui.screens.CustomerCenterScreen
import com.example.ui.screens.HelpVideosScreen
import com.example.ui.screens.HelpVideoType
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.screens.OnboardingNicknameScreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.theme.EmotionDiaryTheme
import com.example.ui.viewmodel.EmotionViewModel
import com.example.ui.viewmodel.ProfileViewModel
import com.example.ui.viewmodel.PasswordChangeViewModel
import com.example.ui.viewmodel.SupportViewModel
import com.example.ui.viewmodel.StoryViewModel
import com.example.ui.viewmodel.RecoveryViewModel
import com.example.ui.viewmodel.AiEmotionAnalysisViewModel
import com.example.ui.viewmodel.CounselingViewModel
import com.example.ui.viewmodel.HelpVideoViewModel
import com.example.ui.viewmodel.HumorTrainingViewModel
import com.example.ui.viewmodel.VocalTrainingViewModel
import com.example.ui.viewmodel.WitTrainingViewModel
import com.example.ui.viewmodel.DatingTrainingViewModel
import com.example.ui.viewmodel.ExpressionTrainingViewModel
import com.example.ui.viewmodel.QuizTrainingViewModel
import com.example.ui.viewmodel.DebateTrainingViewModel

enum class NavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HUMAN_EMOTIONS("인간의 감정", Icons.Default.SelfImprovement, Icons.Outlined.SelfImprovement),
    FAVORITE_EMOTIONS("즐찾감정", Icons.Default.Star, Icons.Outlined.StarBorder),
    EMOTION_DIARY("감정다이어리", Icons.Default.PieChart, Icons.Outlined.PieChartOutline),
    INNER_STORIES("나의 사연", Icons.Default.MenuBook, Icons.Outlined.MenuBook)
}

enum class MainNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("홈", Icons.Default.Home, Icons.Outlined.Home),
    EMOTION_DIARY("감정다이어리", Icons.Default.MenuBook, Icons.Outlined.MenuBook),
    AI_CARE("AI 마음케어", Icons.Default.AutoAwesome, Icons.Outlined.AutoAwesome),
    MY_INFO("내정보", Icons.Default.Person, Icons.Outlined.Person)
}

private enum class HomeDestination {
    HOME,
    INTRODUCTION,
    GUIDE,
    HUMAN_EMOTIONS,
    FAVORITE_EMOTIONS,
    EMOTION_DIARY,
    INNER_STORIES
}

private enum class DiaryDestination {
    MENU,
    HUMAN_EMOTIONS,
    FAVORITE_EMOTIONS,
    EMOTION_DIARY,
    INNER_STORIES,
    RECOVERY
}

private enum class AiCareDestination {
    MENU,
    EMOTION_ANALYSIS,
    COUNSELING,
    TRAINING,
    TRAINING_HUMOR,
    TRAINING_DATING,
    TRAINING_EMOTION_EXPRESSION,
    TRAINING_QUIZ,
    TRAINING_DEBATE,
    TRAINING_VOCAL,
    TRAINING_WIT
}

private enum class MyInfoDestination {
    MENU,
    EDIT_PROFILE,
    CHANGE_PASSWORD,
    HELP_VIDEOS,
    SUPPORT,
    APP_INFO
}

@Composable
private fun ChildDestination(
    title: String,
    onBack: () -> Unit,
    showWoochisoBrand: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        com.example.ui.components.ChildNavigationBar(title = title, onBack = onBack, showWoochisoBrand = showWoochisoBrand)
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

class MainActivity : ComponentActivity() {

    private val viewModel: EmotionViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private val passwordChangeViewModel: PasswordChangeViewModel by viewModels()
    private val supportViewModel: SupportViewModel by viewModels()
    private val storyViewModel: StoryViewModel by viewModels()
    private val recoveryViewModel: RecoveryViewModel by viewModels()
    private val aiEmotionAnalysisViewModel: AiEmotionAnalysisViewModel by viewModels()
    private val counselingViewModel: CounselingViewModel by viewModels()
    private val helpVideoViewModel: HelpVideoViewModel by viewModels()
    private val humorTrainingViewModel: HumorTrainingViewModel by viewModels()
    private val datingTrainingViewModel: DatingTrainingViewModel by viewModels()
    private val expressionTrainingViewModel: ExpressionTrainingViewModel by viewModels()
    private val quizTrainingViewModel: QuizTrainingViewModel by viewModels()
    private val debateTrainingViewModel: DebateTrainingViewModel by viewModels()
    private val vocalTrainingViewModel: VocalTrainingViewModel by viewModels()
    private val witTrainingViewModel: WitTrainingViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            EmotionDiaryTheme {
                val context = LocalContext.current
                var selectedTabIndex by remember { mutableIntStateOf(0) }
                var homeDestination by remember { mutableStateOf(HomeDestination.HOME) }
                var diaryDestination by remember { mutableStateOf(DiaryDestination.MENU) }
                var aiCareDestination by remember { mutableStateOf(AiCareDestination.MENU) }
                var myInfoDestination by remember { mutableStateOf(MyInfoDestination.MENU) }
                var profileReturnTabIndex by remember { mutableStateOf<Int?>(null) }
                var showEmailSignup by remember { mutableStateOf(false) }
                var showForgotPassword by remember { mutableStateOf(false) }
                var pendingStoryDetailId by remember { mutableStateOf<Long?>(null) }
                val authState by authViewModel.uiState.collectAsStateWithLifecycle()
                val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()
                val passwordChangeState by passwordChangeViewModel.state.collectAsStateWithLifecycle()
                val storyState by storyViewModel.state.collectAsStateWithLifecycle()
                LaunchedEffect(authState.session) {
                    if (authState.session != null) storyViewModel.load()
                }

                val favorites by viewModel.favorites.collectAsStateWithLifecycle()
                val diaryEntries by viewModel.diaryEntries.collectAsStateWithLifecycle()
                val filteredDiaryEntries by viewModel.filteredDiaryEntries.collectAsStateWithLifecycle()
                val innerStories by viewModel.innerStories.collectAsStateWithLifecycle()
                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
                val emotionCategoryStats by viewModel.emotionCategoryStats.collectAsStateWithLifecycle()
                val selectedDateRangeText by viewModel.selectedDateRangeText.collectAsStateWithLifecycle()
                val selectedChartRange by viewModel.selectedChartRange.collectAsStateWithLifecycle()

                val userNickname by viewModel.userNickname.collectAsStateWithLifecycle()
                val serverNickname = authState.session?.nickname?.takeIf { it.isNotBlank() }
                val displayNickname = serverNickname ?: userNickname
                var showEditNicknameDialog by remember { mutableStateOf(false) }
                val openProfileFromHeader = {
                    if (selectedTabIndex != MainNavTab.MY_INFO.ordinal) {
                        profileReturnTabIndex = selectedTabIndex
                    }
                    selectedTabIndex = MainNavTab.MY_INFO.ordinal
                    myInfoDestination = MyInfoDestination.EDIT_PROFILE
                }
                val closeProfile = {
                    myInfoDestination = MyInfoDestination.MENU
                    profileReturnTabIndex?.let { selectedTabIndex = it }
                    profileReturnTabIndex = null
                }
                val goHome = {
                    selectedTabIndex = MainNavTab.HOME.ordinal
                    homeDestination = HomeDestination.HOME
                    profileReturnTabIndex = null
                }

                LaunchedEffect(Unit) {
                    viewModel.toastEvent.collect { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }

                LaunchedEffect(profileState.user) {
                    profileState.user?.let { user ->
                        authViewModel.updateCurrentUser(user.email, user.nickname, user.role)
                    }
                }
                LaunchedEffect(profileState.message, profileState.error) {
                    (profileState.message ?: profileState.error)?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        profileViewModel.consumeMessage()
                    }
                }
                LaunchedEffect(profileState.requiresLogin) {
                    if (profileState.requiresLogin) authViewModel.logout()
                }
                LaunchedEffect(passwordChangeState.requiresLogin) {
                    if (passwordChangeState.requiresLogin) authViewModel.logout()
                }
                LaunchedEffect(storyState.message, storyState.error) {
                    (storyState.message ?: storyState.error)?.let {
                        Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
                        storyViewModel.consume()
                    }
                }
                LaunchedEffect(storyState.requiresLogin) {
                    if (storyState.requiresLogin) authViewModel.logout()
                }

                BackHandler(
                    enabled = authState.isLoggedIn &&
                        selectedTabIndex == MainNavTab.HOME.ordinal &&
                        homeDestination != HomeDestination.HOME
                ) { homeDestination = HomeDestination.HOME }
                BackHandler(
                    enabled = authState.isLoggedIn &&
                        selectedTabIndex == MainNavTab.EMOTION_DIARY.ordinal &&
                        diaryDestination != DiaryDestination.MENU
                ) {
                    diaryDestination = DiaryDestination.MENU
                }
                BackHandler(
                    enabled = authState.isLoggedIn &&
                        selectedTabIndex == MainNavTab.AI_CARE.ordinal &&
                        aiCareDestination != AiCareDestination.MENU
                ) {
                    aiCareDestination = when (aiCareDestination) {
                        AiCareDestination.TRAINING_HUMOR,
                        AiCareDestination.TRAINING_DATING,
                        AiCareDestination.TRAINING_EMOTION_EXPRESSION,
                        AiCareDestination.TRAINING_QUIZ,
                        AiCareDestination.TRAINING_DEBATE,
                        AiCareDestination.TRAINING_VOCAL,
                        AiCareDestination.TRAINING_WIT -> AiCareDestination.TRAINING
                        else -> AiCareDestination.MENU
                    }
                }
                BackHandler(
                    enabled = authState.isLoggedIn &&
                        selectedTabIndex == MainNavTab.MY_INFO.ordinal &&
                        myInfoDestination != MyInfoDestination.MENU &&
                        myInfoDestination != MyInfoDestination.SUPPORT
                ) {
                    if (myInfoDestination == MyInfoDestination.EDIT_PROFILE && profileReturnTabIndex != null) {
                        closeProfile()
                    } else {
                        myInfoDestination = MyInfoDestination.MENU
                    }
                }

                if (!authState.isLoggedIn && showForgotPassword) {
                    ForgotPasswordScreen(
                        state = authState,
                        onSubmit = authViewModel::requestPasswordReset,
                        onBack = { authViewModel.resetPasswordResetState(); showForgotPassword = false }
                    )
                } else if (!authState.isLoggedIn && showEmailSignup) {
                    SignupScreen(
                        state = authState,
                        onSubmit = { request -> authViewModel.register(request) { showEmailSignup = false } },
                        onGoogleSignup = { token -> showEmailSignup = false; authViewModel.loginWithGoogle(token) },
                        onGoogleSignupFailed = authViewModel::googleLoginFailed,
                        onEmailChanged = authViewModel::resetEmailCheck,
                        onCheckEmail = authViewModel::checkEmail,
                        onNicknameChanged = authViewModel::resetNicknameCheck,
                        onCheckNickname = authViewModel::checkNickname,
                        onBack = { showEmailSignup = false }
                    )
                } else if (!authState.isLoggedIn && authState.googleSignup != null) {
                    GoogleSignupScreen(
                        state = authState,
                        signup = authState.googleSignup!!,
                        onSubmit = authViewModel::completeGoogleSignup,
                        onCancel = authViewModel::cancelGoogleSignup
                    )
                } else if (!authState.isLoggedIn) {
                    LoginScreen(
                        state = authState,
                        onLogin = authViewModel::login,
                        onGoogleLogin = authViewModel::loginWithGoogle,
                        onGoogleLoginFailed = authViewModel::googleLoginFailed,
                        onInputChanged = authViewModel::clearError,
                        onOpenSignup = { showEmailSignup = true },
                        onForgotPassword = { authViewModel.resetPasswordResetState(); showForgotPassword = true }
                    )
                } else if (displayNickname.isNullOrBlank()) {
                    OnboardingNicknameScreen(
                        onNicknameSaved = { newNickname ->
                            viewModel.saveUserNickname(newNickname)
                        }
                    )
                } else {
                    val isHomeVideoDestination = selectedTabIndex == MainNavTab.HOME.ordinal &&
                        (homeDestination == HomeDestination.INTRODUCTION || homeDestination == HomeDestination.GUIDE)
                    val mainAccentColor = if (selectedTabIndex == MainNavTab.HOME.ordinal) {
                        Color(0xFF74AFDD)
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                    val mainAccentContainerColor = if (selectedTabIndex == MainNavTab.HOME.ordinal) {
                        Color(0xFFEAF2FB)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    }
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        topBar = {
                            if (!isHomeVideoDestination) {
                            TopAppBar(
                                title = {
                                    Row(
                                        modifier = Modifier.clickable(onClick = goHome),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_uchiso_app_icon_1785309286513),
                                            contentDescription = "App Icon",
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "감정다이어리",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF064AD6),
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${displayNickname?.takeIf { it.isNotBlank() } ?: "테스트 사용자"}님 반가워요 ^^",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.clickable(onClick = openProfileFromHeader)
                                            )
                                        }
                                    }
                                },
                                actions = {
                                    Surface(
                                        onClick = openProfileFromHeader,
                                        shape = RoundedCornerShape(16.dp),
                                        color = mainAccentContainerColor,
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Face,
                                                contentDescription = null,
                                                tint = mainAccentColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = displayNickname ?: "",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = mainAccentColor
                                            )
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "별명 수정",
                                                tint = mainAccentColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }
                                    IconButton(onClick = authViewModel::logout) {
                                        Icon(
                                            imageVector = Icons.Default.Logout,
                                            contentDescription = "로그아웃",
                                            tint = mainAccentColor
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                            }
                        },
                    bottomBar = {
                        if (!isHomeVideoDestination) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            windowInsets = NavigationBarDefaults.windowInsets
                        ) {
                            MainNavTab.entries.forEachIndexed { index, tab ->
                                val isSelected = selectedTabIndex == index
                                NavigationBarItem(
                                    selected = isSelected,
                                    onClick = {
                                        selectedTabIndex = index
                                        if (tab == MainNavTab.HOME) {
                                            goHome()
                                        }
                                        if (tab == MainNavTab.EMOTION_DIARY) {
                                            diaryDestination = DiaryDestination.MENU
                                        }
                                        if (tab == MainNavTab.AI_CARE) {
                                            aiCareDestination = AiCareDestination.MENU
                                        }
                                        if (tab == MainNavTab.MY_INFO) {
                                            profileReturnTabIndex = null
                                            myInfoDestination = MyInfoDestination.MENU
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = Color(0xFF74AFDD),
                                        selectedTextColor = Color(0xFF74AFDD),
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        indicatorColor = Color(0xFFEAF2FB)
                                    )
                                )
                            }
                        }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Crossfade(targetState = selectedTabIndex, label = "tab_crossfade") { tabIndex ->
                            when (MainNavTab.entries[tabIndex]) {
                                MainNavTab.HOME -> when (homeDestination) {
                                    HomeDestination.HOME -> HomeScreen(
                                        nickname = displayNickname,
                                        onRecordEmotion = { homeDestination = HomeDestination.HUMAN_EMOTIONS },
                                        onOpenIntroduction = { homeDestination = HomeDestination.INTRODUCTION },
                                        onOpenGuide = { homeDestination = HomeDestination.GUIDE },
                                        onOpenHumanEmotions = { homeDestination = HomeDestination.HUMAN_EMOTIONS },
                                        onOpenFavoriteEmotions = { homeDestination = HomeDestination.FAVORITE_EMOTIONS },
                                        onOpenEmotionDiary = { homeDestination = HomeDestination.EMOTION_DIARY },
                                        onOpenInnerStories = { homeDestination = HomeDestination.INNER_STORIES },
                                        onOpenAiCare = {
                                            aiCareDestination = AiCareDestination.MENU
                                            selectedTabIndex = MainNavTab.AI_CARE.ordinal
                                        }
                                    )
                                    HomeDestination.INTRODUCTION -> VideoPlayerScreen(
                                        videoType = HelpVideoType.INTRO,
                                        title = "우치소 소개",
                                        viewModel = helpVideoViewModel,
                                        onBack = { homeDestination = HomeDestination.HOME }
                                    )
                                    HomeDestination.GUIDE -> VideoPlayerScreen(
                                        videoType = HelpVideoType.EMOTION_DIARY,
                                        title = "감정다이어리 사용법",
                                        viewModel = helpVideoViewModel,
                                        onBack = { homeDestination = HomeDestination.HOME }
                                    )
                                    HomeDestination.HUMAN_EMOTIONS -> ChildDestination("인간의 감정", { homeDestination = HomeDestination.HOME }) { HumanEmotionsScreen(
                                        viewModel = viewModel,
                                        selectedCategory = selectedCategoryFilter,
                                        showPageTitle = false
                                    ) }
                                    HomeDestination.FAVORITE_EMOTIONS -> ChildDestination("즐찾감정", { homeDestination = HomeDestination.HOME }) { FavoriteEmotionsScreen(
                                        viewModel = viewModel,
                                        favorites = favorites,
                                        innerStories = innerStories,
                                        onOpenInnerStories = { homeDestination = HomeDestination.INNER_STORIES },
                                        showPageTitle = false
                                    ) }
                                    HomeDestination.EMOTION_DIARY -> ChildDestination("감정그래프", { homeDestination = HomeDestination.HOME }) { EmotionDiaryScreen(
                                        viewModel = viewModel,
                                        diaryEntries = filteredDiaryEntries,
                                        emotionCategoryStats = emotionCategoryStats,
                                        selectedDateRangeText = selectedDateRangeText,
                                        selectedRange = selectedChartRange,
                                        innerStories = innerStories,
                                        serverStories = storyState.stories,
                                        loginNickname = displayNickname,
                                        onOpenStoryDetail = { storyId ->
                                            pendingStoryDetailId = storyId
                                            homeDestination = HomeDestination.INNER_STORIES
                                        },
                                        showPageTitle = false
                                    ) }
                                    HomeDestination.INNER_STORIES -> ChildDestination("나의 사연", { homeDestination = HomeDestination.HOME }) { InnerStoriesScreen(
                                        viewModel = storyViewModel,
                                        initialStoryId = pendingStoryDetailId,
                                        onInitialStoryHandled = { pendingStoryDetailId = null },
                                        showPageTitle = false
                                    ) }
                                }
                                MainNavTab.EMOTION_DIARY -> when (diaryDestination) {
                                    DiaryDestination.MENU -> EmotionDiaryHubScreen(
                                        onOpenHumanEmotions = { diaryDestination = DiaryDestination.HUMAN_EMOTIONS },
                                        onOpenFavoriteEmotions = { diaryDestination = DiaryDestination.FAVORITE_EMOTIONS },
                                        onOpenEmotionDiary = { diaryDestination = DiaryDestination.EMOTION_DIARY },
                                        onOpenInnerStories = { diaryDestination = DiaryDestination.INNER_STORIES },
                                        onOpenRecovery = { diaryDestination = DiaryDestination.RECOVERY }
                                    )
                                    DiaryDestination.HUMAN_EMOTIONS -> ChildDestination("인간의 감정", { diaryDestination = DiaryDestination.MENU }) { HumanEmotionsScreen(
                                        viewModel = viewModel,
                                        selectedCategory = selectedCategoryFilter,
                                        showPageTitle = false
                                    ) }
                                    DiaryDestination.FAVORITE_EMOTIONS -> ChildDestination("즐찾감정", { diaryDestination = DiaryDestination.MENU }) { FavoriteEmotionsScreen(
                                        viewModel = viewModel,
                                        favorites = favorites,
                                        innerStories = innerStories,
                                        onOpenInnerStories = { diaryDestination = DiaryDestination.INNER_STORIES },
                                        showPageTitle = false
                                    ) }
                                    DiaryDestination.EMOTION_DIARY -> ChildDestination("감정그래프", { diaryDestination = DiaryDestination.MENU }) { EmotionDiaryScreen(
                                        viewModel = viewModel,
                                        diaryEntries = filteredDiaryEntries,
                                        emotionCategoryStats = emotionCategoryStats,
                                        selectedDateRangeText = selectedDateRangeText,
                                        selectedRange = selectedChartRange,
                                        innerStories = innerStories,
                                        serverStories = storyState.stories,
                                        loginNickname = displayNickname,
                                        onOpenStoryDetail = { storyId ->
                                            pendingStoryDetailId = storyId
                                            diaryDestination = DiaryDestination.INNER_STORIES
                                        },
                                        showPageTitle = false
                                    ) }
                                    DiaryDestination.INNER_STORIES -> ChildDestination("나의 사연", { diaryDestination = DiaryDestination.MENU }) { InnerStoriesScreen(
                                        viewModel = storyViewModel,
                                        initialStoryId = pendingStoryDetailId,
                                        onInitialStoryHandled = { pendingStoryDetailId = null },
                                        showPageTitle = false
                                    ) }
                                    DiaryDestination.RECOVERY -> ChildDestination("감정회복", { diaryDestination = DiaryDestination.MENU }) { RecoveryScreen(
                                        viewModel = recoveryViewModel,
                                        onAuthExpired = authViewModel::logout,
                                        showPageTitle = false
                                    ) }
                                }
                                MainNavTab.AI_CARE -> when (aiCareDestination) {
                                    AiCareDestination.MENU -> AiCareScreen(
                                        onOpenEmotionAnalysis = { aiCareDestination = AiCareDestination.EMOTION_ANALYSIS },
                                        onOpenCounseling = { aiCareDestination = AiCareDestination.COUNSELING },
                                        onOpenTraining = { aiCareDestination = AiCareDestination.TRAINING }
                                    )
                                     AiCareDestination.EMOTION_ANALYSIS -> ChildDestination("AI 감정분석", { aiCareDestination = AiCareDestination.MENU }) { AiEmotionAnalysisScreen(
                                         viewModel = aiEmotionAnalysisViewModel,
                                         onAuthExpired = authViewModel::logout,
                                         nickname = authState.session?.nickname ?: displayNickname,
                                         showPageTitle = false
                                     ) }
                                    AiCareDestination.COUNSELING -> ChildDestination("AI 상담", {
                                        if (!counselingViewModel.navigateBack()) aiCareDestination = AiCareDestination.MENU
                                    }) { AiCounselingScreen(
                                        viewModel = counselingViewModel,
                                        onAuthExpired = authViewModel::logout,
                                        showPageTitle = false
                                    ) }
                                    AiCareDestination.TRAINING -> ChildDestination("AI 트레이닝", { aiCareDestination = AiCareDestination.MENU }) { AiTrainingScreen(
                                        showPageTitle = false,
                                        onOpenHumor = { aiCareDestination = AiCareDestination.TRAINING_HUMOR },
                                        onOpenDating = { aiCareDestination = AiCareDestination.TRAINING_DATING },
                                        onOpenEmotionExpression = { aiCareDestination = AiCareDestination.TRAINING_EMOTION_EXPRESSION },
                                        onOpenQuiz = { aiCareDestination = AiCareDestination.TRAINING_QUIZ },
                                        onOpenDebate = { aiCareDestination = AiCareDestination.TRAINING_DEBATE },
                                        onOpenVocal = { aiCareDestination = AiCareDestination.TRAINING_VOCAL },
                                        onOpenWit = { aiCareDestination = AiCareDestination.TRAINING_WIT }
                                    ) }
                                    AiCareDestination.TRAINING_HUMOR -> ChildDestination("😂 AI를 웃겨라", { aiCareDestination = AiCareDestination.TRAINING }, true) {
                                        HumorTrainingScreen(
                                            viewModel = humorTrainingViewModel,
                                            onAuthExpired = authViewModel::logout
                                        )
                                    }
                                    AiCareDestination.TRAINING_DATING -> ChildDestination("💕 AI 소개팅", { aiCareDestination = AiCareDestination.TRAINING }, true) {
                                        DatingTrainingScreen(
                                            viewModel = datingTrainingViewModel,
                                            onAuthExpired = authViewModel::logout
                                        )
                                    }
                                    AiCareDestination.TRAINING_EMOTION_EXPRESSION -> ChildDestination("💬 AI와 감정표현 연습", { aiCareDestination = AiCareDestination.TRAINING }, true) {
                                        ExpressionTrainingScreen(
                                            viewModel = expressionTrainingViewModel,
                                            onAuthExpired = authViewModel::logout
                                        )
                                    }
                                    AiCareDestination.TRAINING_QUIZ -> ChildDestination("🧠 AI 상식퀴즈", { aiCareDestination = AiCareDestination.TRAINING }, true) {
                                        QuizTrainingScreen(
                                            viewModel = quizTrainingViewModel,
                                            onAuthExpired = authViewModel::logout
                                        )
                                    }
                                    AiCareDestination.TRAINING_DEBATE -> ChildDestination("⚖️ AI 토론연습", { aiCareDestination = AiCareDestination.TRAINING }, true) { DebateTrainingScreen(debateTrainingViewModel, authViewModel::logout) }
                                    AiCareDestination.TRAINING_VOCAL -> ChildDestination("🎤 AI 보컬트레이닝", { aiCareDestination = AiCareDestination.TRAINING }, true) { VocalTrainingScreen(vocalTrainingViewModel, authViewModel::logout) }
                                    AiCareDestination.TRAINING_WIT -> ChildDestination("💡 AI 재치와 센스", { aiCareDestination = AiCareDestination.TRAINING }, true) { WitTrainingScreen(witTrainingViewModel, authViewModel::logout) }
                                }
                                MainNavTab.MY_INFO -> when (myInfoDestination) {
                                    MyInfoDestination.MENU -> MyInfoScreen(
                                        nickname = authState.session?.nickname ?: displayNickname,
                                        email = authState.session?.email,
                                        grade = authState.session?.grade,
                                        onEditProfile = { profileReturnTabIndex = null; myInfoDestination = MyInfoDestination.EDIT_PROFILE },
                                        onChangePassword = { myInfoDestination = MyInfoDestination.CHANGE_PASSWORD },
                                        onOpenHelpVideos = { myInfoDestination = MyInfoDestination.HELP_VIDEOS },
                                        onOpenSupport = { myInfoDestination = MyInfoDestination.SUPPORT },
                                        onOpenAppInfo = { myInfoDestination = MyInfoDestination.APP_INFO },
                                        onLogout = authViewModel::logout
                                    )
                                    MyInfoDestination.EDIT_PROFILE -> {
                                        ChildDestination("회원정보 수정", { closeProfile() }) { EditProfileScreen(
                                            state = profileState,
                                            onLoad = profileViewModel::loadProfile,
                                            onSave = profileViewModel::updateProfile,
                                            showPageTitle = false
                                        ) }
                                    }
                                    MyInfoDestination.CHANGE_PASSWORD -> ChildDestination("비밀번호 변경", { myInfoDestination = MyInfoDestination.MENU }) { ChangePasswordScreen(
                                        state = passwordChangeState,
                                        onSubmit = passwordChangeViewModel::change,
                                        onSuccess = {
                                            Toast.makeText(context, passwordChangeState.message ?: "비밀번호가 변경되었습니다.", Toast.LENGTH_SHORT).show()
                                            passwordChangeViewModel.reset()
                                            myInfoDestination = MyInfoDestination.MENU
                                        },
                                        showPageTitle = false
                                    ) }
                                    MyInfoDestination.HELP_VIDEOS -> ChildDestination("사용방법 영상", { myInfoDestination = MyInfoDestination.MENU }) { HelpVideosScreen(helpVideoViewModel) }
                                    MyInfoDestination.SUPPORT -> CustomerCenterScreen(
                                        viewModel = supportViewModel,
                                        onAuthExpired = authViewModel::logout,
                                        onExit = { myInfoDestination = MyInfoDestination.MENU }
                                    )
                                    MyInfoDestination.APP_INFO -> ChildDestination("앱 정보", { myInfoDestination = MyInfoDestination.MENU }) { AppInfoScreen() }
                                }
                            }
                        }
                    }
                }

                if (showEditNicknameDialog && serverNickname == null && !userNickname.isNullOrBlank()) {
                    var editInput by remember { mutableStateOf(userNickname ?: "") }
                    AlertDialog(
                        onDismissRequest = { showEditNicknameDialog = false },
                        title = {
                            Text(
                                text = "별명 수정",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        text = {
                            Column {
                                Text(
                                    text = "새로 사용하실 별명을 입력해 주세요.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = editInput,
                                    onValueChange = { editInput = it },
                                    label = { Text("별명 / 닉네임") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = com.example.ui.theme.AppActionButton,
                                    contentColor = Color.White
                                ),
                                onClick = {
                                    if (editInput.trim().isNotBlank()) {
                                        viewModel.updateNickname(editInput)
                                        showEditNicknameDialog = false
                                    }
                                }
                            ) {
                                Text("저장")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditNicknameDialog = false }) {
                                Text("취소")
                            }
                        }
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun MainTabPlaceholder(title: String, onBack: (() -> Unit)? = null, showTitle: Boolean = true) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (showTitle) Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (onBack != null) {
            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Text("홈으로")
            }
        }
    }
}

