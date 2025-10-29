package it.pagopa.io.app.cie_example.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import it.pagopa.io.app.cie.CieSDK
import it.pagopa.io.app.cie_example.BuildConfig
import it.pagopa.io.app.cie_example.MainActivity
import it.pagopa.io.app.cie_example.R
import it.pagopa.io.app.cie_example.ui.header.BackArrowIcon
import it.pagopa.io.app.cie_example.ui.header.HeaderImage
import it.pagopa.io.app.cie_example.ui.header.HomeIcon
import it.pagopa.io.app.cie_example.ui.header.MailIcon
import it.pagopa.io.app.cie_example.ui.header.hideAll
import it.pagopa.io.app.cie_example.ui.model.MailModel
import it.pagopa.io.app.cie_example.ui.model.NisAndPaceReadDto
import it.pagopa.io.app.cie_example.ui.model.NisDto
import it.pagopa.io.app.cie_example.ui.model.PaceReadDto
import it.pagopa.io.app.cie_example.ui.view.CieSdkMethods
import it.pagopa.io.app.cie_example.ui.view.HomeView
import it.pagopa.io.app.cie_example.ui.view.NisAndPaceReadView
import it.pagopa.io.app.cie_example.ui.view.NisAndPaceView
import it.pagopa.io.app.cie_example.ui.view.NisReadView
import it.pagopa.io.app.cie_example.ui.view.PaceProtocol
import it.pagopa.io.app.cie_example.ui.view.PaceReadView
import it.pagopa.io.app.cie_example.ui.view.ReadCie
import it.pagopa.io.app.cie_example.ui.view.ReadNis
import it.pagopa.io.app.cie_example.ui.view_model.CieSdkMethodsViewModel
import it.pagopa.io.app.cie_example.ui.view_model.NisAndPaceReadViewModel
import it.pagopa.io.app.cie_example.ui.view_model.NisAndPaceViewModel
import it.pagopa.io.app.cie_example.ui.view_model.NisReadViewModel
import it.pagopa.io.app.cie_example.ui.view_model.PaceReadViewModel
import it.pagopa.io.app.cie_example.ui.view_model.PaceViewModel
import it.pagopa.io.app.cie_example.ui.view_model.ReadCieViewModel
import it.pagopa.io.app.cie_example.ui.view_model.ReadNisViewModel
import it.pagopa.io.app.cie_example.ui.view_model.dependenciesInjectedViewModel
import kotlin.reflect.typeOf

@Composable
fun MainActivity?.CieSdkNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    headerLeft: MutableState<HeaderImage?>,
    titleResId: MutableIntState,
    headerRight: MutableState<HeaderImage?>
) {
    NavHost(
        navController = navController,
        modifier = Modifier.padding(innerPadding),
        startDestination = Routes.Home
    ) {
        composable<Routes.Home> {
            listOf(headerLeft, headerRight).hideAll()
            titleResId.intValue = R.string.app_name
            HomeView(onClick = {
                navController.navigate(Routes.CieSdkMethods)
            })
        }
        composable<Routes.CieSdkMethods> {
            headerLeft.BackArrowIcon(navController)
            headerRight.HomeIcon(navController)
            titleResId.intValue = R.string.test_methods_title
            val ctx = LocalContext.current
            val cieSdk = CieSDK.withContext(ctx)
            val vm = dependenciesInjectedViewModel<CieSdkMethodsViewModel>(cieSdk)
            CieSdkMethods(vm, onNavigate = {
                navController.navigate(Routes.ReadCIE)
            }, onNavigateToNisAuth = {
                navController.navigate(Routes.ReadNIS)
            }, onNavigateToPaceAuth = {
                navController.navigate(Routes.PaceAuth)
            }, onInitNisAndPace = {
                navController.navigate(Routes.NisAndPaceAuth)
            })
        }
        composable<Routes.ReadCIE> {
            headerLeft.BackArrowIcon(navController)
            headerRight.HomeIcon(navController)
            titleResId.intValue = R.string.auth_with_cie
            val ctx = LocalContext.current
            val cieSdk = CieSDK.withContext(ctx).withCustomIdpUrl(BuildConfig.BASE_URL_IDP)
            val vm = dependenciesInjectedViewModel<ReadCieViewModel>(cieSdk)
            ReadCie(vm)
        }

        composable<Routes.ReadNIS> {
            headerLeft.BackArrowIcon(navController)
            headerRight.HomeIcon(navController)
            titleResId.intValue = R.string.reading_nis
            val cieSdk = CieSDK.withContext(LocalContext.current)
            val vm = dependenciesInjectedViewModel<ReadNisViewModel>(cieSdk)
            ReadNis(vm){
                navController.navigate(Routes.NisRead(it))
            }
        }
        composable<Routes.PaceAuth> {
            headerLeft.BackArrowIcon(navController)
            headerRight.HomeIcon(navController)
            titleResId.intValue = R.string.reading_pace
            val cieSdk = CieSDK.withContext(LocalContext.current)
            val vm = dependenciesInjectedViewModel<PaceViewModel>(cieSdk)
            PaceProtocol(
                vm,
                onNavigateToPaceRead = {
                    navController.navigate(Routes.PaceRead(it))
                }
            )
        }
        composable<Routes.NisAndPaceAuth> {
            headerLeft.BackArrowIcon(navController)
            headerRight.HomeIcon(navController)
            titleResId.intValue = R.string.reading_nis_and_pace
            val cieSdk = CieSDK.withContext(LocalContext.current)
            val vm = dependenciesInjectedViewModel<NisAndPaceViewModel>(cieSdk)
            NisAndPaceView(
                viewModel = vm,
                onNavigateToNisAndPaceRead = {
                    navController.navigate(Routes.NisAndPaceRead(it))
                }
            )
        }
        composable<Routes.NisRead>(
            typeMap = mapOf(typeOf<NisDto>() to CustomNavType.nisDtoReadType)
        ) {
            val arguments = it.toRoute<Routes.NisRead>()
            headerLeft.BackArrowIcon(navController)
            val mailSubject = stringResource(R.string.mail_subject_nis)
            headerRight.MailIcon { recipients ->
                this@CieSdkNavHost?.sendMail(
                    MailModel(
                        recipients = recipients.toList(),
                        subject = mailSubject,
                        body = arguments.nisDto.mailMessage()
                    )
                )
            }
            titleResId.intValue = R.string.reading_nis
            val vm = dependenciesInjectedViewModel<NisReadViewModel>(arguments.nisDto)
            NisReadView(vm)
        }
        composable<Routes.PaceRead>(
            typeMap = mapOf(typeOf<PaceReadDto>() to CustomNavType.paceReadType)
        ) {
            val arguments = it.toRoute<Routes.PaceRead>()
            headerLeft.BackArrowIcon(navController)
            val mailSubject = stringResource(R.string.mail_subject_pace)
            headerRight.MailIcon { recipients ->
                this@CieSdkNavHost?.sendMail(
                    MailModel(
                        recipients = recipients.toList(),
                        subject = mailSubject,
                        body = arguments.paceReadDto.mailMessage()
                    )
                )
            }
            titleResId.intValue = R.string.reading_pace
            val vm = dependenciesInjectedViewModel<PaceReadViewModel>(arguments.paceReadDto)
            PaceReadView(vm)
        }
        composable<Routes.NisAndPaceRead>(
            typeMap = mapOf(typeOf<NisAndPaceReadDto>() to CustomNavType.nisAndPaceReadType)
        ) {
            val arguments = it.toRoute<Routes.NisAndPaceRead>()
            headerLeft.BackArrowIcon(navController)
            val mailSubject = stringResource(R.string.mail_subject)
            headerRight.MailIcon { recipients ->
                this@CieSdkNavHost?.sendMail(
                    MailModel(
                        recipients = recipients.toList(),
                        subject = mailSubject,
                        body = arguments.nisAndPaceReadDto.mailMessage()
                    )
                )
            }
            titleResId.intValue = R.string.reading_pace
            val vm = dependenciesInjectedViewModel<NisAndPaceReadViewModel>(
                arguments.nisAndPaceReadDto
            )
            NisAndPaceReadView(vm)
        }
    }
}
