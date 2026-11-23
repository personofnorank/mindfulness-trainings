# Mindfulness Trainings

Welcome to the Mindfulness Trainings app.

This app is designed to help you practise with either the Five or the Fourteen Mindfulness Trainings of the Plum Village tradition of engaged Buddhism, by displaying the name of a new training in the notification bar of your phone or tablet every day. Tapping on the notification brings up the full text of the training in the app.

The aim is to help those of us who have to use devices like this in our daily life bear the trainings in mind and live more mindfully.

## Building

Requires JDK 17 (see `gradle.properties`) and an Android SDK with platform 35. Clone the repository, then:

```sh
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Licence

The app's source code is licensed under the [GPL-3.0](LICENSE). The texts of the
Mindfulness Trainings themselves are © Thich Nhat Hanh / Plum Village and are
distributed here as-is; they may not be modified, and modified texts may not be
distributed under this app's name or with any implied association with Plum
Village. See [NOTICE](NOTICE) for details.
