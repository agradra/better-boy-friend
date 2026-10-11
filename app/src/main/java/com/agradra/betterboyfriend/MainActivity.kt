package com.agradra.betterboyfriend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.agradra.betterboyfriend.ui.screens.MainScreen
import com.agradra.betterboyfriend.ui.screens.TriggerScreen
import com.agradra.betterboyfriend.ui.theme.BetterBoyFriendTheme
import com.agradra.betterboyfriend.ui.viewmodel.RuleViewModel
import com.agradra.betterboyfriend.ui.viewmodel.TriggerViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {

    private val ruleViewModel: RuleViewModel by viewModels()
    private val triggerViewModel: TriggerViewModel by viewModels()

    private val exportCsvLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                ruleViewModel.exportToCsv(outputStream)
            }
        }
    }

    private val importCsvLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                ruleViewModel.importFromCsv(inputStream)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BetterBoyFriendTheme {
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }

                val appBottomBar: @Composable () -> Unit = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = {
                                Icon(Icons.AutoMirrored.Filled.FormatListBulleted, contentDescription = "행동 지침")
                            },
                            label = { Text("행동 지침") }
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = {
                                Icon(Icons.Default.Bolt, contentDescription = "트리거")
                            },
                            label = { Text("트리거") }
                        )
                    }
                }

                if (selectedTab == 0) {
                    MainScreen(
                        viewModel = ruleViewModel,
                        onExportCsvClick = {
                            val fileName = "better-boy-friend_rules_${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.csv"
                            exportCsvLauncher.launch(fileName)
                        },
                        onImportCsvClick = {
                            importCsvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "text/plain", "*/*"))
                        },
                        bottomBar = appBottomBar
                    )
                } else {
                    TriggerScreen(
                        viewModel = triggerViewModel,
                        bottomBar = appBottomBar
                    )
                }
            }
        }
    }
}