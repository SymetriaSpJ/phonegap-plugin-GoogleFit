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

#### Uruchamiamy proces uzyskania praw do Google Fit API

```javascript
window.plugins.fitatuGoogleFit.getPermissions(
  function() {
	console.log('success');
  },
  function(message) {
    console.log('error: ' + message);
  }
);
```

#### Pobieranie aktywności

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
  activityStartedAt: "2016-05-12 08:35",
  activityStoppedAt: "2016-05-12 08:55",
  appName: "Endomondo",
  distance: 1311.4349365234375,
  energy: 94,
  name: "running",
  source: "com.endomondo.android"
],
[
  aivityStartedAt: "2016-05-12 12:32"
  activityStoppedAt: "2016-05-12 12:47"
  appName: "Endomondo"
  distance: 1283.7640380859375
  energy: 92
  name: "running"
  source: "com.endomondo.android"
]

```

PS. Do zmiennej message trafiają komunikaty przeznaczone tylko dla oczu programisty ;)