## FTP extractor for KBC

Extractor component for Keboola Connection allowing to retrieve files from FTP
server and store them in Storage. 

##Funcionality

Simple component allowing user to dowload specified CSV files from FTP
repository. In current version the component allows to retrieve files
in several scenarios:

- Retrieve	single specified file and store it in specified Storage table 
- Retrieve all CSV files existing in specified directory and upload them to a
	single storage table. 
- Retrieve all CSV files with specified prefix from specified directory and
	upload them to a single storage table. 

Extractor retrieves only files that has changed or has been added since the
last retrieval. All specified content is downloaded at the first run.

### Configuration parameters
Configuration requires in default 3 basic credential parameters (`ftpUrl`, `user`, `pass`) and 3 mapping parameters (`ftpPath`, `sapiPath`, `pkey`). All other parameters that are not marked as required *(REQ)* are optional and set to default values specified below. `pkey` parameter is required only if the`incremental` is set to 1 (default).

- **ftpUrl** – (REQ) url of FTP host  
- **user** – (REQ) FTP user name  
- **#pass** (REQ) FTP password 
- **mappings** - (REQ) list of files (folders) to 	download 
    - **ftpPath** - (REQ) remote Path (by default a
		remote folder) 
    - **sapiPath** – (REQ) Sapi path where the result
		will be uploaded (e.g. out.c-main.mytable) 
    - **isFolder** – (DEFAULT 0) indicator whether to
		download a single file or the whole folder 
    - **incremental** – (DEFAULT 1) specifies whether
		to upload incrementally. If not set to 0, the pkey must be
		specified. 		 
    - **pkey** – (REQ) – name of the primary key column, required (by default0  if incremental is set to 1 
    - **prefix** – optional prefix string of files to	download and group into single table 
    - **delimiter** – (DEFAULT ,) delimiter remote csv file	(default , ) 
    - **enclosure** – (DEFAULT ") enclosure of remote csv file

### Sample configurations / use cases

#### Use case 1

Simplest setting of the extractor. Download all
csv files in the remote folders `downloads` and `downloads2` and store
them incrementaly in a specified storage table. Assuming that the
delimiter and enclosure is default `,` and `“`.

    {
        "user": "user1",
        "#pass": "mypassword",
        "mappings": [
          {
            "ftpPath": "downloads",
            "sapiPath": "out.c-main.mytable1",
            "pkey": "ID"
          },
          {
            "ftpPath": "downloads2",
            "sapiPath": "out.c-main.mytable2",
            "pkey": "asd"
          }
        ]
      }
      
#### Use case 2

Downlad all csv files in the `downloads` directory which start with orders. e.g. files `orders_1-1-12`, `orders-5` will be downloaded and uploaded to specified table `out.c-main.order`.

    {
        "user": "user1",
        "#pass": "mypassword",
        "mappings": [
          {
            "ftpPath": "downloads",
            "sapiPath": "out.c-main.orders",
            "pkey": "ID",
            "prefix": "orders”,
          }]
      }
#### Use case 3
Downlad single csv file from remote path
`downloads/single.csv`. 

    {
        "user": "user1",
        "#pass": "mypassword",
        "mappings": [
          {
            "ftpPath": "downloads/single.csv",
            "sapiPath": "out.c-main.single",
            "isFolder" : 0,
            "pkey": "ID"
          }]
      }
