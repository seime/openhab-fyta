# Fyta Beam Binding

This binding links [Fyta Beam plant sensors](https://fyta.de/en) via the cloud.

<img src="logo.png"  width="20%" height="20%">
<br/>
<img src="theme.png"  width="20%" height="20%">

## Supported Things

* `account` = Fyta account
* `plant` = A plant with a Fyta Beam sensor

> Not tested sensors *without* a hub. If you have this, please enable DEBUG logging and send details to author.

## Discovery

Create a `account` bridge and perform discovery. Your registered plants with Beam sensors will appear.

## Thing Configuration

See full example below for how to configure using thing files.

To find the `macAddress`, add the bridge and let it discover your locks.

### Account Thing Configuration

* `email` = Email address used in the mobile app
* `password` = Same as you use in the mobile app

### Plant Thing configuration

* `macAddress` = MAC address of Beam sensor

## Channels

Also see Thing properties. Plant names etc can be found there.

| Channel                      | Type                   | Read/Write | Description                                                            |
|------------------------------|------------------------|------------|------------------------------------------------------------------------|
| `nickname`                   | `String`               | R          | User provided plant nickname                                           |
| `thumbnail`                  | `Image`                | R          | Thumbnail image of plant (provided by user)                            |
| `temperature-status`         | `String`               | R          | Plant happyness with temperature. See plant happyness codes below      |
| `temperature`                | `Number:Temperature`   | R          | Average temperature last hour                                          |
| `temperature-min-acceptable` | `Number:Temperature`   | R          | Min acceptable temp                                                    |
| `temperature-min-good`       | `Number:Temperature`   | R          | Min good temp                                                          |
| `temperature-max-good`       | `Number:Temperature`   | R          | Max god temp                                                           |
| `temperature-max-acceptable` | `Number:Temperature`   | R          | Max acceptable temp                                                    |
| `moisture-status`            | `String`               | R          | Plant happyness with soil moisture. See plant happyness codes below    |
| `moisture`                   | `Number:Dimensionless` | R          | Average soil moisture/humidity last hour                               |
| `moisture-min-acceptable`    | `Number:Dimensionless` | R          | Min acceptable soil moisture/humidity                                  |
| `moisture-min-good`          | `Number:Dimensionless` | R          | Min good soil moisture/humidity                                        |
| `moisture-max-good`          | `Number:Dimensionless` | R          | Max god soil moisture/humidity                                         |
| `moisture-max-acceptable`    | `Number:Dimensionless` | R          | Max acceptable soil moisture/humidity                                  |
| `salinity-status`            | `String`               | R          | Plant happyness with soil salinity. See plant happyness codes below    |
| `salinity`                   | `Number`               | R          | Average soil salinity last hour                                        |
| `salinity-min-acceptable`    | `Number`               | R          | Min acceptable soil salinity                                           |
| `salinity-min-good`          | `Number`               | R          | Min good soil salinity                                                 |
| `salinity-max-good`          | `Number`               | R          | Max god soil salinity                                                  |
| `salinity-max-acceptable`    | `Numbers`              | R          | Max acceptable soil salinity                                           |
| `light-status`               | `String`               | R          | Plant happyness with light conditions. See plant happyness codes below |
| `light`                      | `Number`               | R          | Average light amount last hour                                         |
| `battery`                    | `Number:Dimensionless` | R          | Remaining battery percentage                                           |
| `last-updated`               | `DateTime`             | R          | Last data received from sensor                                         |

### Plant happyness codes

* `TOO_LOW`
* `LOW`
* `PERFECT` - aim for this
* `HIGH`
* `TOO_HIGH`

## Full Example

### Thing Configuration

```
Bridge fyta:account:account  "Fyta Account Bridge" [ email="***********", password="********"] { 
    Thing plant beam_1 "Palm plant" [macAddress="00:AA:BB:CC:DD:EE"]
    Thing plant beam_2 "Lizzie Izzy" [macAddress="11:AA:BB:CC:DD:EE"]
}
```

### Item Configuration

```
String Sensor_Fyta_1_Name "Palm image" <image>                                       {channel="fyta:plant:account:beam_1:nickname"}
Image Sensor_Fyta_1_Thumbnail "Palm image" <image>                                   {channel="fyta:plant:account:beam_1:lthumbnail"}

// Temperature
String Sensor_Fyta_1_Status_Temperature "Palm temp status" <QualityOfService>        {channel="fyta:plant:account:beam_1:temperature-status"}
Number:Temperature Sensor_Fyta_1_Temperature "Palm temp [%d %unit%]" <temperature>   {channel="fyta:plant:account:beam_1:temperature"}
Number:Temperature Sensor_Fyta_1_Temperature_Min_Acceptable "Palm min acceptable temp [%d %unit%]" <temperature> (gRestoreOnStartup) {channel="fyta:plant:account:beam_1:temperature-min-acceptable", expire="2h,state=UNDEF"}
Number:Temperature Sensor_Fyta_1_Temperature_Min_Good "Palm min good temp [%d %unit%]" <temperature> (gRestoreOnStartup) {channel="fyta:plant:account:beam_1:temperature-min-good", expire="2h,state=UNDEF"}
Number:Temperature Sensor_Fyta_1_Temperature_Max_Good "Palm max good temp [%d %unit%]" <temperature> (gRestoreOnStartup) {channel="fyta:plant:account:beam_1:temperature-max-good", expire="2h,state=UNDEF"}
Number:Temperature Sensor_Fyta_1_Temperature_Max_Acceptable "Palm max acceptable temp [%d %unit%]" <temperature> (gRestoreOnStartup) {channel="fyta:plant:account:beam_1:temperature-max-acceptable", expire="2h,state=UNDEF"}

// Moisture
String Sensor_Fyta_1_Status_Moisture "Palm moisture status" <QualityOfService>       {channel="fyta:plant:account:beam_1:moisture-status"}
Number:Dimensionless Sensor_Fyta_1_Moisture "Palm soil moisture" <humidity>     {channel="fyta:plant:account:beam_1:moisture"}
Number:Dimensionless Sensor_Fyta_1_Moisture_Min_Acceptable "Palm min acceptable moisture [%d %unit%]" <humidity> (gRestoreOnStartup) {channel="fyta:plant:account:beam_1:moisture-min-acceptable", expire="2h,state=UNDEF"}
Number:Dimensionless Sensor_Fyta_1_Moisture_Min_Good "Palm min good moisture [%d %unit%]" <humidity> (gRestoreOnStartup) {channel="fyta:plant:account:beam_1:moisture-min-good", expire="2h,state=UNDEF"}
Number:Dimensionless Sensor_Fyta_1_Moisture_Max_Good "Palm max good moisture [%d %unit%]" <humidity> (gRestoreOnStartup) {channel="fyta:plant:account:beam_1:moisture-max-good", expire="2h,state=UNDEF"}
Number:Dimensionless Sensor_Fyta_1_Moisture_Max_Acceptable "Palm max acceptable moisture [%d %unit%]" <humidity> (gRestoreOnStartup) {channel="fyta:plant:account:beam_1:moisture-max-acceptable", expire="2h,state=UNDEF"}

// Salinity
String Sensor_Fyta_1_Status_Salinity "Palm salinity status" <QualityOfService>       {channel="fyta:plant:account:beam_1:salinity-status"}
Number Sensor_Fyta_1_Soil_Salinity "Palm salinity" <oil>                             {channel="fyta:plant:account:beam_1:salinity"}
Number Sensor_Fyta_1_Salinity_Min_Acceptable "Palm min acceptable salinity [%d %unit%]" <oil> (gRestoreOnStartup) {channel="fyta:plant:account:beam_1:salinity-min-acceptable", expire="2h,state=UNDEF"}
Number Sensor_Fyta_1_Salinity_Min_Good "Palm min good salinity [%d %unit%]" <oil> (gRestoreOnStartup) {channel="fyta:plant:account:beam_1:salinity-min-good", expire="2h,state=UNDEF"}
Number Sensor_Fyta_1_Salinity_Max_Good "Palm max good salinity [%d %unit%]" <oil> (gRestoreOnStartup) {channel="fyta:plant:account:beam_1:salinity-max-good", expire="2h,state=UNDEF"}
Number Sensor_Fyta_1_Salinity_Max_Acceptable "Palm max acceptable salinity [%d %unit%]" <oil> (gRestoreOnStartup) {channel="fyta:plant:account:beam_1:salinity-max-acceptable", expire="2h,state=UNDEF"}

// Light
String Sensor_Fyta_1_Status_Light "Palm light status" <QualityOfService>             {channel="fyta:plant:account:beam_1:light-status"}
Number Sensor_Fyta_1_Light "Palm sunlight" <sun>                                     {channel="fyta:plant:account:beam_1:light"}

Number:Dimensionless Sensor_Fyta_1_Battery "Palm" <battery>                          {channel="fyta:plant:account:beam_1:battery"}
DateTime Sensor_Fyta_1_Last_Updated "Palm last updated" <time>                       {channel="fyta:plant:account:beam_1:last-updated"}
```
