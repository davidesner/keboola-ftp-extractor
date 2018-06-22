## FTP(S)/SFTP extractor for KBC
Extractor component for Keboola Connection allowing to retrieve files from FTP
server and store them in Storage. 

## Funcionality

Simple component allowing user to dowload specified CSV files from FTP
repository. In current version the component allows to retrieve files
in several scenarios:

- Retrieve	single specified file and store it in specified Storage table 
- Retrieve all CSV files existing in specified directory and upload them to a
	single storage table. 
- Retrieve all CSV files with specified prefix from specified directory and
	upload them to a single storage table. 

Extractor retrieves only files that has changed or has been added since the
last retrieval. All specified content is downloaded at the first run. Note that this is based on remote files timestamps and you need to set the parameter *FTP host timezone*  properly in order to make this work for FTP(S) protocol. 

The extractor now supports FTP, FTPS and SFTP protocols.

### FTP over SSL, Explicit FTPS
The component allows both implicit FTP over SSL (default port 990) and explicit FTPS (default port 21). By default it supports all certificates signed by Root CAs.
### Compression
The component newly allows to retrieve archived (ZIP) files. You can download a single zip file or all files in the folder in the same manner as when downloading csv files. I.e. you can specify `prefix` parameter or a full name. The only difference is that the `extension` parameter is ignored and the ZIP file is expected to contain only csv files of the same type - all files within the archive are merged into the single Storage table. The ZIP files must not contain folder hierarchy and all folders are ignored.

### Configuration parameters
- **FTP host URL** – (REQ) url of FTP host
- **FTP host port** - optional parameter allowing to specify alternative port. If null, the default port for selected protocol is used.  
- **FTP user name** – (REQ) FTP user name  
- **FTP password** – (REQ) FTP password 
- **FTP protocol** – (REQ) Specifies the FTP protocol (FTP, FTPS_IMPLICIT, FTPS_EXPLICIT and SFTP are currently supported) 
- **FTP host timezone** – (REQ) timezone of the FTP host machine. This parameter needs to be specified for the extractor to work properly. Due to FTP protocol limitation, the timestamps of remote files reflects the timezone setting of the remote server, however the TZ information is not passed by any way. If this is not set correctly, the extractor may not retrieve all newly uploaded files, since it is based on their timestamps. (NOTE: this parameter does not affect SFTP protocol) 
- **mappings** - (REQ) list of files (folders) to 	download 
    - **Remote Path** - (REQ) remote Path (by default a
		remote folder) e.g. /myfolder/mysubfolder
    - **Table name** – (REQ) Storage bucket and table where the result will be uploaded (e.g. out.c-main.mytable).
    - **Download folder or file** – (DEFAULT FOLDER) indicator whether to
		download a single file or the whole folder 
    - **Compression** - (DEFAULT `None`) Specifies whether the retrieved files are archives and its' compression type. For more information see Compression section above.
    - **extension** - (DEFAULT `csv`) optional parameter specifying custom extension of files to retrieve. i.e. `txt`. If not specified all files with `csv` extension will be downloaded by default. **NOTE:** Wildcard `*` may be specified to accept all extensions or for files with none.
    - **Storage upload mode** – (DEFAULT INCREMENTAL) specifies whether
		to upload incrementally. If set to INCREMENTAL, the pkey must be
		specified. 		 
    - **Primary Key** – (REQ) – array of namees of the primary key columns, required (If upload mode is INCREMENTAL)
    - **Prefix** – optional prefix string of files to	download and group into single table 
    - **Delimiter** – (DEFAULT ,) delimiter remote csv file	(default , ) 
    - **Enclosure** – (DEFAULT ") enclosure of remote csv file

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
            "pkey": ["ID"]
          },
          {
            "ftpPath": "downloads2",
            "sapiPath": "out.c-main.mytable2",
            "pkey": ["asd"]
          }
        ]
      }
      
#### Use case 2

Downlad all csv files in the `downloads` directory whose name start with `orders`. e.g. files `orders_1-1-12.csv`, `orders-5.csv` will be downloaded and uploaded to specified table `out.c-main.order`.

    {
        "user": "user1",
        "#pass": "mypassword",
        "mappings": [
          {
            "ftpPath": "downloads",
            "sapiPath": "out.c-main.orders",
            "pkey": ["ID"],
            "prefix": "orders”,
          }]
      }
#### Use case 3

Downlad all csv files in the `downloads` directory whose name start with `orders` and its' extension is `txt`. e.g. files `orders_1-1-12.txt`, `orders-5.txt` will be downloaded and uploaded to specified table `out.c-main.order`.

    {
        "user": "user1",
        "#pass": "mypassword",
        "mappings": [
          {
            "ftpPath": "downloads",
            "sapiPath": "out.c-main.orders",
            "pkey": ["ID"],
            "prefix": "orders”,
            "extension": "txt" 
          }]
      }
#### Use case 4
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
            "pkey": ["ID"]
          }]
      }