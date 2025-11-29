package com.example.myapplication

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.network.ApiService
import com.example.myapplication.ui.FormScreen
import com.example.myapplication.ui.HistoryScreen
import com.example.myapplication.ui.theme.MyApplicationTheme  // Replace with your theme package
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : ComponentActivity() {

    private val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.BASE_API) // your backend base URL
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            MyApplicationTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MainScreen(api = api)
                }
            }
        }
    }
}

@Composable
fun MainScreen(api: ApiService) {

    val context = LocalContext.current

    var selectedTab by remember { mutableStateOf(0) }

    val tabs = listOf("Create", "History")

    Column {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> FormScreen(context, api)
            1 -> HistoryScreen(api)
        }
    }
}

