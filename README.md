Google Fit phonegap plugin
=========

#### Sprawdzanie czy udało się nawiązać połączenie z Google Fit API

Jest to równoznaczne z tym, że użytkownik ma wszystkie potrzebne prawa.

```javascript
window.plugins.fitatuGoogleFit.isConnected(
  function(isConnected) {
	if (isConnected) {
	  console.log('success');
	} else {
	  console.log('do not have permission');
	}
  }
);
```

#### Sprawdzamy czy posiadamy prawa do lokalizacji i aktywności

```javascript
window.plugins.fitatuGoogleFit.hasPermission(
  function(hasPermission) {
  	if (hasPermission) {
  	  console.log('You have permission');
  	} else {
  	  console.log("You don't have permission");
  	}
  },
  function(message) {
    console.log('error: ' + message);
  }
);
```

#### Uruchamiamy proces uzyskania praw do lokalizacji i aktywności

```javascript
window.plugins.fitatuGoogleFit.getPermission(
  function(currentPermission) {
  	if (currentPermission) {
  	  console.log('You have permission already');
  	} else {
  	  console.log('Popup was displayed');
  	}
  },
  function(message) {
    console.log('error: ' + message);
  }
);
```

#### Uruchamiamy proces uzyskania praw do Google Fit API

```javascript
window.plugins.fitatuGoogleFit.getGoogleFitPermission(
  function() {
	console.log('success');
  },
  function(message) {
    console.log('error: ' + message);
  }
);
```

#### Ustawiamy wzrost i wagę w Google Fit

Ustawienie wagi i wzrostu jest niezbędne aby odbierać aktywności nagrywane w Google Fit.
Dlatego warto wywołać te funkcję od razu po nawiązaniu połączenia. 

```javascript
window.plugins.fitatuGoogleFit.setUserSettings(
  70,  // weight in kg
  172, // height in cm
  function(){
      console.log('success');
  },
  function(message){
      console.log('error: ' + message );
  }
);
```

#### Pobieranie wszystkich aktywności

```javascript
window.plugins.fitatuGoogleFit.getActivities(
  1463037099000, // startTime in milliseconds
  1463090099000, // endTime in milliseconds
  function(activites) {
    console.log(activites);
  },
  function(message) {
    console.log('error: ' + message);
  }
);
```

Przykład activites:
```
[
    {
      activityStartedAt: "2016-05-12 08:35:32", // UTC
      activityStoppedAt: "2016-05-12 08:55:40",
      distance: 1311.4349365234375,
      energy: 94,
      name: "running",
      type: 8,
      source: "com.endomondo.android",
      applicationName: "Endomondo"
    },
    {
      aivityStartedAt: "2016-05-12 12:32:23",
      activityStoppedAt: "2016-05-12 12:47:32",
      distance: 1283.7640380859375,
      energy: 92,
      name: "running",
      type: 8,
      source: "com.endomondo.android",
      applicationName: "Endomondo"
    }
]

```

#### Pobieranie aktywności z Google Fit (Fit i GMS wraz z aktywnościami dziennymi)

```javascript
window.plugins.fitatuGoogleFit.getGMSActivities(
  1463037099000, // startTime in milliseconds
  1463090099000, // endTime in milliseconds
  function(activites) {
    console.log(activites);
  },
  function(message) {
    console.log('error: ' + message);
  }
);
```

Przykład activites:
```
[
    {
      activityStartedAt: "2016-05-12 08:35:32", // UTC
      activityStoppedAt: "2016-05-12 08:55:40",
      distance: 1311.4349365234375,
      energy: 94,
      name: "running",
      type: 8,
      source: "com.google.android.apps.fitness",
      applicationName: "Fit"
    },
    {
      aivityStartedAt: "2016-05-12 12:32:23",
      activityStoppedAt: "2016-05-12 12:47:32",
      distance: 1283.7640380859375,
      energy: 92,
      name: "running",
      type: 8,
      source: "com.google.android.gms",
      applicationName: "Usługi Google Play"
    },
    {
      aivityStartedAt: "2016-05-12 12:32:23",
      activityStoppedAt: "2016-05-12 12:47:32",
      distance: 1283.7640380859375,
      energy: 92,
      daily: true,
      source: "com.google.android.gms",
      applicationName: "Usługi Google Play"
    }
]

```

#### Pobieranie dziennej aktywności z Google Fit

```javascript
window.plugins.fitatuGoogleFit.getGMSDailyActivities(
  1463037099000, // startTime in milliseconds
  1463090099000, // endTime in milliseconds
  function(activites) {
    console.log(activites);
  },
  function(message) {
    console.log('error: ' + message);
  }
);
```

Przykład activites:
```
[
    {
      acivityStartedAt: "2016-05-12 12:32:23",
      activityStoppedAt: "2016-05-12 12:47:32",
      distance: 1283.7640380859375,
      energy: 1692,
      basalEnergy: 1332,
      activeCalories: 360,
      daily: true,
      source: "com.google.android.gms",
      applicationName: "Usługi Google Play",
      steps: 789
    }
]

```

#### Pobieranie poziomu podstawowej przemiany materii z Google Fit z ostatnich 24 miesięcy

```javascript
window.plugins.fitatuGoogleFit.getBMRValues(
  1463090099000, // endTime in milliseconds
  function(activites) {
    console.log(activites);
  },
  function(message) {
    console.log('error: ' + message);
  }
);
```

Przykład activites:
```
[
    {
      basalEnergy: 1234.21,
      date: "2019-05-12 12:23:32"
    },
    {
      basalEnergy: 1432.56,
      date: "2019-07-13 12:23:32"
    }
]

```

UWAGA: do message trafiają komunikaty przeznaczone tylko dla oczu programisty ;)


#### Proponowany flow pobierania uprawnień:

![Proponowany flow pobierania uprawnień](https://github.com/SymetriaSpJ/phonegap-plugin-GoogleFit/blob/docs/GoogleFitPhonegapPlugin-getPermissions.png "getPermissions")


