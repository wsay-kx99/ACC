package app.akilesh.qacc.ui

import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import app.akilesh.qacc.Const.getAssetFiles
import app.akilesh.qacc.R
import app.akilesh.qacc.databinding.ActivityMainBinding
import app.akilesh.qacc.utils.DownloadUtils.download
import app.akilesh.qacc.viewmodel.InstallApkViewModel
import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.enums.AppUpdaterError
import com.github.javiersantos.appupdater.enums.UpdateFrom.JSON
import com.github.javiersantos.appupdater.objects.Update
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class MainActivity: AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appUpdaterUtils: AppUpdaterUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val decorView = window.decorView
        decorView.systemUiVisibility = WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS

        when (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
            Configuration.UI_MODE_NIGHT_NO -> {
                decorView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.fragment) as NavHostFragment
        val navController = navHostFragment.navController


         // Hide bottom app bar & ext. fab while creating an accent
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when(destination.id) {
                R.id.color_picker, R.id.dark_accent, R.id.customisation -> {
                    binding.bottomAppBar.visibility = View.GONE
                    binding.xFab.visibility = View.GONE
                }
                else -> {
                    binding.bottomAppBar.visibility = View.VISIBLE
                    binding.xFab.visibility = View.VISIBLE
                }
            }
        }

        val navOptions = navOptions {
            anim  {
                // Animations from Android 10
                enter  = R.anim.fragment_enter
                exit = R.anim.fragment_exit
                popEnter = R.anim.fragment_enter_pop
                popExit = R.anim.fragment_exit_pop
            }
        }

        binding.xFab.setOnClickListener {
            navController.navigate(R.id.color_picker, null, navOptions)
        }

        binding.bottomAppBar.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.settings -> navController.navigate(R.id.settings, null, navOptions)
                R.id.info -> navController.navigate(R.id.info, null, navOptions)
            }
            true
        }

        /*
         * Use navigation icon to navigate home.
         * May not be the correct way, but convenient.
         */
        binding.bottomAppBar.setNavigationOnClickListener {
            navController.navigate(R.id.home, null, navOptions)
        }

        copyAssets()

        appUpdaterUtils = AppUpdaterUtils(this)

        appUpdaterUtils
            .setUpdateFrom(JSON)
            .setUpdateJSON("https://raw.githubusercontent.com/Akilesh-T/ACC/master/app/update-changelog.json")
            .withListener(object : AppUpdaterUtils.UpdateListener {

                     override fun onSuccess(update: Update?, isUpdateAvailable: Boolean?) {
                         if (isUpdateAvailable!!) {
                             binding.updateCard.visibility = View.VISIBLE
                             val url = "${update!!.urlToDownload}/download/acc-v${update.latestVersion}.apk"
                             binding.update.setOnClickListener {
                                 MaterialAlertDialogBuilder(this@MainActivity)
                                     .setTitle("What's new in v${update.latestVersion}:")
                                     .setMessage(update.releaseNotes)
                                     .setPositiveButton(getString(R.string.dl_and_install)) { _, _ ->
                                         val model = ViewModelProvider(this@MainActivity).get(
                                             InstallApkViewModel::class.java)
                                         Toast.makeText(this@MainActivity, "Downloading v${update.latestVersion}", Toast.LENGTH_SHORT).show()
                                         download(this@MainActivity, url, update.latestVersion,  model)
                                     }
                                     .create()
                                     .show()
                             }
                         }
                         else binding.updateCard.visibility = View.GONE
                     }

                     override fun onFailed(error: AppUpdaterError?) {
                         Log.e("UpdaterError", error.toString())
                     }
                 }
            )
            .start()
    }

    private fun copyAssets() {
        if( !File("$filesDir/src/values").exists() )
            File("$filesDir/src/values").mkdirs()

        val assetFiles = getAssetFiles()
        assetFiles.forEach {
            val file = it.removeSuffix("64")
            assets.open(file).use { stream ->
                File("${filesDir}/$file").outputStream().use { fileOutputStream ->
                    stream.copyTo(fileOutputStream)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        appUpdaterUtils.start()
    }

    override fun onStop() {
        super.onStop()
        appUpdaterUtils.stop()
    }
}