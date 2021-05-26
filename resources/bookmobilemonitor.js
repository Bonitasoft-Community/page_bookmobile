'use strict';
/**
 *
 */

(function() {


var appCommand = angular.module('bookmobilemonitor', ['ui.bootstrap','ngSanitize', 'ngMaterial', 'ngCookies']); 
                                                                    

// --------------------------------------------------------------------------
//
// Controler BookMobile
//
// --------------------------------------------------------------------------

// Ping the server
appCommand.controller('BookMobileController',
  
	function ( $http, $scope, $sce, $filter,  $cookies  ) { // 

	this.listevents='';
	this.inprogress=false;
	
	this.getHttpConfig = function () {
		var additionalHeaders = {};
		var csrfToken = $cookies.get('X-Bonita-API-Token');
		if (csrfToken) {
			additionalHeaders ['X-Bonita-API-Token'] = csrfToken;
		}
		var config= {"headers": additionalHeaders};
		console.log("GetHttpConfig : "+angular.toJson( config));
		return config;
	}

	this.navbaractiv = 'catalog';

	this.showlistcatalog=true;
	
	this.getNavClass = function( tabtodisplay )
	{
		if (this.navbaractiv === tabtodisplay)
			return 'ng-isolate-scope active';
		return 'ng-isolate-scope';
	}

	this.getNavStyle = function( tabtodisplay )
	{
		if (this.navbaractiv === tabtodisplay)
			return 'border: 1px solid #c2c2c2;border-bottom-color: transparent;';
		return 'background-color:#cbcbcb';
	}
	this.listcatalog = [];
	this.listbdmavailable = [];
	
	this.init = function()
	{
		
		var self=this;
		self.inprogress=true;
		var d = new Date();
		
		$http.get( '?page=custompage_bookmobile&action=init&t='+d.getTime(), this.getHttpConfig() )
				.success( function ( jsonResult, statusHttp, headers, config ) {
					// connection is lost ?
					if (statusHttp==401 || typeof jsonResult === 'string') {
						console.log("Redirected to the login page !");
						window.location.reload();
					}
					console.log("init",jsonResult);
					self.inprogress=false;
					self.listcatalog 		= jsonResult.listcatalog;
					self.listbdmavailable	= jsonResult.listbdmavailable;
				})
				.error( function() {
					self.inprogress=false;
					});				
	}
	
	this.init()
	// -----------------------------------------------------------------------------------------
	//  										Add Model
	// -----------------------------------------------------------------------------------------

	this.showModelPanel=""; // '', 'model', 'bdm'
	
	this.model = {};
	this.marker = "emty";
	this.editmodellistevents="";
	
	this.addModel = function ()	{
		this.showModelPanel = 'model';

		this.model={ columns: [], type:'model' };
	}
	
	this.addBdm = function() {
		this.showModelPanel = 'bdm';
		this.model={ columns: [],  
			type:'model', 
			datasourceName:'java:comp/env/NotManagedBizDataDS',  
			colPersistenceIdName : 'PERSISTENCEID'};
		
	}
	this.editModel = function( modelid) {
		console.log("BookMobile.getModel= "+ modelid);
		var self=this;
		
		var d = new Date();
		this.inprogress=true;
		$http.get( '?page=custompage_bookmobile&action=getModel&id='+modelid+'&t='+d.getTime(), this.getHttpConfig() )
		.success( function ( jsonResult, statusHttp, headers, config ) {
			// connection is lost ?
			if (statusHttp==401 || typeof jsonResult === 'string') {
				console.log("Redirected to the login page !");
				window.location.reload();
			}
			console.log("editmodel",jsonResult);
			self.inprogress=false;
			self.model 		= jsonResult.model;
			self.editmodellistevents = jsonResult.listevents;
			if (self.model.type==='BDM') {					
				self.showModelPanel = "bdm";
			} else {
				self.showModelPanel = "model";
			}

		})
		.error( function() {
			self.inprogress=false;
			});				
	}

	this.populateFromTable =function() {
		console.log("BookMobile.populateFromTable "+JSON.stringify(  this.model));
		
		var param = { "id": this.model.id,
				"tablename": this.model.tablename
				};
		var json = encodeURIComponent( angular.toJson( param, false));
		
		var self=this;
		this.inprogress=true;
		var d = new Date();
		$http.get( '?page=custompage_bookmobile&action=populateFromTable&paramjson='+json+'&t='+d.getTime(), this.getHttpConfig() )
		.success( function ( jsonResult, statusHttp, headers, config ) {
			// connection is lost ?
			if (statusHttp==401 || typeof jsonResult === 'string') {
				console.log("Redirected to the login page !");
				window.location.reload();
			}
			console.log("populatefromtable",jsonResult);
			self.inprogress=false;
			// do not update all, user may have modify the name, the description...
			self.model.columns 								= jsonResult.model.columns;
			self.model.colPersistenceIdName 				= jsonResult.model.colPersistenceIdName
			self.editmodellistevents 						= jsonResult.listevents;
		})
		.error( function() {
			self.inprogress=false;
			});			
	} 
	
	this.updateModel = function() {
		console.log("BookMobile.updateModel "+JSON.stringify(  this.model));
		
		this.inprogress=true;
		var d = new Date();
		this.sendPost("updateModel", this.model, this.updateModelCallback);
	}
		
	this.updateModelCallback = function ( jsonResult, self) {
		console.log("updateModelCallback marker ="+this.marker+" self="+self.marker);
		self.listcatalog 					= jsonResult.listcatalog;
		self.model.id 						= jsonResult.model.id;
		self.editmodellistevents 			= jsonResult.listevents;
	}
	
	this.addModelColumn = function() {
		var recordToAdd = { 'type': 'STRING', 'size':100} ; 
		this.model.columns.push(  recordToAdd );
	}
	
	this.removeModelColumn = function( column ) {
		var index =this.model.columns.indexOf( column );
		this.model.columns.splice(index, 1);     
	}
		// -----------------------------------------------------------------------------------------
	//  										Bdm
	// -----------------------------------------------------------------------------------------
	this.changeBdm = function() {
		this.inprogress=true;
		var self=this;
		var d = new Date();
		$http.get( '?page=custompage_bookmobile&action=getBdmDefinition&name='+this.model.name+'&t='+d.getTime(), this.getHttpConfig() )
			.success( function ( jsonResult, statusHttp, headers, config ) {
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page !");
					window.location.reload();
				}
				console.log("getBdmDefinition",jsonResult);
				self.model 					= jsonResult.model;
				self.editmodellistevents 	= jsonResult.listevents;
				self.inprogress=false;
			})
		.error( function() {
			self.inprogress=false;
			});			
	}

	this.dropModel = function( id, name, type ) {
		this.inprogress=true;
		var self=this;
		var d = new Date();
		$http.get( '?page=custompage_bookmobile&action=dropmodel&id='+id+'&name='+name+'&type='+type+'&t='+d.getTime(), this.getHttpConfig() )
			.success( function ( jsonResult, statusHttp, headers, config ) {
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page !");
					window.location.reload();
				}
				console.log("getBdmDefinition",jsonResult);
				self.listcatalog 		= jsonResult.listcatalog;
				self.inprogress=false;
			})
		.error( function() {
			self.inprogress=false;
			});			
	}
	// -----------------------------------------------------------------------------------------
	//  										Use  Model
	// -----------------------------------------------------------------------------------------
	this.showmodel = null;
	this.useModel = function ( model ) {
		var self=this;
		this.showmodel = model;
		this.showlistcatalog= false;

		this.inprogress=true;
		var d = new Date();
		$http.get( '?page=custompage_bookmobile&action=getModel&id='+this.showmodel.id+'&t='+d.getTime(), this.getHttpConfig() )
			.success( function ( jsonResult, statusHttp, headers, config ) {
				// connection is lost ?
				if (statusHttp==401 || typeof jsonResult === 'string') {
					console.log("Redirected to the login page !");
					window.location.reload();
				}
				console.log("getModel",jsonResult);
				self.inprogress=false;
				self.showmodel 		= jsonResult.model;
				self.showmodellistevents = jsonResult.listevents;
			})
		.error( function() {
			self.inprogress=false;
			});			
	}

	this.formmodel = {};
	
	this.searchDatas = function() {
			console.log("BookMobile.searchRecords "+JSON.stringify( this.formmodel ));
			this.searchlistevents="";
			
			this.inprogress=true;
			var d = new Date();
			var param = {
					'id': this.showmodel.id,
					'form' : this.formmodel
			}
			this.sendPost("searchDatas", param, this.searchDatasCallback);
		}
			
		this.searchDatasCallback = function ( jsonResult, self) {
			console.log("searchRecordsCallback ");
			self.listdatas				= jsonResult.datas;
			self.searchlistevents 		= jsonResult.listevents;
		}
	

	// -----------------------------------------------------------------------------------------
	//  										Data management
	// -----------------------------------------------------------------------------------------
	this.data={};
	this.showeditdata=false;
	
	this.addData = function() {
		console.log("BookMobile.addRecord "+JSON.stringify( this.data));
		this.updatelistevents= "";
		this.inprogress=true;
		
		var param = {
				'id': this.showmodel.id,
				'data' : this.browserToServer()
		}
		this.sendPost("addData", param, this.addDataCallback);
	}
			
	this.addDataCallback = function ( jsonResult, self) {
		console.log("addRecordCallback marker ="+this.marker+" self="+self.marker);
		self.inprogress=false;
		
		self.updatelistevents 			= jsonResult.listevents;
	}
	
	this.editData = function( item ) {
		this.showAddData=false;
		this.showeditdata=true; 
		this.serverToBrowser( item );
	}
	
	
	// user click on the button. the ctrl.data must be populate from the item 
	// but some transformation are necessary. Example, the date is available as 2012-12-02 but must be transformed to  "2021-05-19T07:00:00.000Z"
	this.serverToBrowser= function( item ) {
		this.data={};
		// By default, copy all values so we have all hidden value, like the persistenceid
		this.data = JSON.parse(JSON.stringify(item)); 
		let offset = new Date().getTimezoneOffset();

		// then, we recalculate the value according the type
		
		for (var i in this.showmodel.columns) {
			let column = this.showmodel.columns[ i ];
			let colvalue = item[ column.name ];
			if (colvalue) {
				if (column.type === 'LOCALDATE' && colvalue.length>0) {
					let colvaluest =	colvalue+"T00:00:00.000";
					colvalue = new Date( colvaluest );
					// Date is then transforme according the timezone on the browser.
					// Example (browser in California :  "2021-04-26" ==> "2021-04-26T07:00:00.000Z")
				}
				if (column.type === 'LOCALDATETIME' && colvalue.length>0) {
					debugger;
					this.data[ column.name+"_orig" ] = colvalue;
					let colvaluest =	colvalue+".000";
					colvalue = new Date( colvaluest );
				}
				if (column.type === 'OFFSETDATETIME' && colvalue.length>0) {
					debugger;		
					this.data[ column.name+"_orig" ] = colvalue;
					// the date was transformed in the local time by the widget. So, 
					// Date on server is "2021-05-20T21:49:00.000"
					// then the new Date(2021-05-20T21:49:00.000 GMT-7) which is incorrect : 
					//   ==> we wanted  2021-05-20T21:49:00.000 UTC
					//   ==> or  2021-05-20T14:49:00.000 GTM-7
					// so, we remove the Offset to get the correct time

					let colvaluest =	colvalue.substring(0,colvalue.length-1)+".000";
					let dateServer = new Date( colvaluest );
					colvalue = new Date( dateServer.getTime() - offset*60000); 
					
				}
			}
			this.data[ column.name ] = colvalue;
		}
	}
	
		// user click on the button. the ctrl.data must be populate from the item 
	// but some transformation are necessary. Example, the date is available as 2012-12-02 but must be transformed to  "2021-05-19T07:00:00.000Z"
	this.browserToServer= function(  ) {
		debugger;
		let offset = new Date().getTimezoneOffset();

		// by default, copy the original value, then we collect all hidden value, like the persistenceid
		let transform =  JSON.parse(JSON.stringify(this.data));
		// then, we transform it

		for (var i in this.showmodel.columns) {
			let column = this.showmodel.columns[ i ];
			let colvalue = this.data[ column.name ];
			if (colvalue) {
				if (column.type === 'LOCALDATE') {
					// date was moved to the local time to display the correct date.
					// Example, May 21 == transformed ==> "2021-05-21T07:00:00.000Z" to display the correct value or 21
					// now, we just collect the first part, no matter the time zone
					// collect "2021-05-21"
					let colvaluest = colvalue.toISOString();
					colvalue = colvaluest.substring(0,10);
				}
				if (column.type === 'LOCALDATETIME' ) {
					debugger;
					// to retrieve the value
					// Date on server is "2021-05-20T21:49:00.000"
					// Browser value is  "2021-05-20T21:49:00.000 GTM-7" ==> We translate the date to the current time zone, else the widget display a different time
					// a toISOString will transform the date to UTC ==> we get a incorrect time.
					// so, now, we have to translate this date in UTC : what time 21:49 GMT-7 is in UTC ? 14:49 GTM-7
					// toISOString( 14:49 GTM-7 ) ==> 21:49:00Z
					
					let dateServer =new Date( colvalue.getTime() - offset*60000);
					let dateServerSt =  dateServer.toISOString();
					// to respect the Bonita format, we have to remove the Z at the end
					// expected format is "2021-04-26T15:40:00"
					colvalue = dateServerSt.substring(0,19);

					// let colvaluest =	colvalue+".000";
					// colvalue = new Date( colvaluest );
				}
				if (column.type === 'OFFSETDATETIME' ) {
					debugger;					
					// the date was transformed in the local time by the widget. So, 
					// Date on server is "2021-05-20T21:49:00.000"
					// Widget display 14:49 GMT-7
					// transform this 14:49 toISOString => 21:49:00Z that what we want

					// colvalue = new Date( colvaluest );
					let dateServerSt =  colvalue.toISOString();	
					colvalue = dateServerSt;
				
				}
			}
			transform[ column.name ] = colvalue;
		}	
		return transform;
	}

	this.updateData = function() {
		console.log("BookMobile.updateData "+JSON.stringify( this.data));
		this.updatelistevents= "";
		this.inprogress=true;
		
		var param = {
				'id': this.showmodel.id,
				'data' :  this.browserToServer()
		}
		this.sendPost("updateData", param, this.updateDataCallback);
	}
	
	this.updateDataCallback = function ( jsonResult, self) {
		console.log("updateDataCallback");
		self.inprogress=false;
		self.updatelistevents 			= jsonResult.listevents;
	}
	
	this.getListColums = function() {
		return this.showmodel.columns;
		/*
		var listColumns = [];
		
		for(col in this.showmodel.columns) {
			if (showAddData && ! col.addReadOnly)
				listColumns.add( col );
			if (! showAddData && ! col.updateReadOnly)
				listColumns.add( col );
		}
		return listColumns;
		*/
	}
	
	/**
	*/
	this.getColumnLabel = function( column ) {
		console.log("getColumnLabel= "+column);
		if (column.label && column.label.length > 0 )
			return column.label;
		return column.name;
	}
	
	this.getDataDefaultValue = function() {
		var data={};
		for (var column in this.showmodel.columns) {
			data[ column.name] = column.addDefaultValue;
		}
		return data;
	}
	/**
	*/
	this.isColumnReadOnly = function( column ) {

		//----------------- insert
		if (this.showAddData) {
			if (column.addSqlValue && column.addSqlValue.length>0)
				return true;
			if (column.addReadOnly ==true)
				return true;
			return false;
		}
		//----------------- update
		// this is the keyId and this is a update ?
		if (this.showmodel.colPersistenceIdName === column.name)
			return true;
		 if (column.updateReadOnly) 
		 	return true;
		 return false;
	}
	// -----------------------------------------------------------------------------------------
	//  										Autocomplete
	// -----------------------------------------------------------------------------------------
	this.autocomplete={};
	
	this.queryUser = function(searchText) {
		var self=this;
		console.log("QueryUser HTTP CALL["+searchText+"]");
		
		self.autocomplete.inprogress=true;
		self.autocomplete.search = searchText;
		self.inprogress=true;
		
		var param={ 'userfilter' :  self.autocomplete.search};
		
		var json = encodeURIComponent( angular.toJson( param, false));
		// 7.6 : the server force a cache on all URL, so to bypass the cache, then create a different URL
		var d = new Date();
		
		return $http.get( '?page=custompage_ping&action=queryusers&paramjson='+json+'&t='+d.getTime(), this.getHttpConfig() )
		.then( function ( jsonResult, statusHttp, headers, config ) {
			// connection is lost ?
			if (statusHttp==401 || typeof jsonResult === 'string') {
				console.log("Redirected to the login page !");
				window.location.reload();
			}
			console.log("QueryUser HTTP SUCCESS.1 - result= "+angular.toJson(jsonResult, false));
			self.autocomplete.inprogress=false;
		 	self.autocomplete.listUsers =  jsonResult.data.listUsers;
			console.log("QueryUser HTTP SUCCESS length="+self.autocomplete.listUsers.length);
			self.inprogress=false;
	
			return self.autocomplete.listUsers;
		},  function ( jsonResult ) {
		console.log("QueryUser HTTP THEN");
		});

	  };
	  
	  
	  
	// -----------------------------------------------------------------------------------------
	//  										Excel
	// -----------------------------------------------------------------------------------------

	this.exportData = function () 
	{  
		//Start*To Export SearchTable data in excel  
	// create XLS template with your field.  
		var mystyle = {         
        headers:true,        
			columns: [  
			{ columnid: 'name', title: 'Name'},
			{ columnid: 'version', title: 'Version'},
			{ columnid: 'state', title: 'State'},
			{ columnid: 'deployeddate', title: 'Deployed date'},
			],         
		};  
	
        //get current system date.         
        var date = new Date();  
        $scope.CurrentDateTime = $filter('date')(new Date().getTime(), 'MM/dd/yyyy HH:mm:ss');          
		var trackingJson = this.listprocesses
        //Create XLS format using alasql.js file.  
        alasql('SELECT * INTO XLS("Process_' + $scope.CurrentDateTime + '.xls",?) FROM ?', [mystyle, trackingJson]);  
    };
    

	
	// Manage the event 
	this.getListEvents = function ( listevents ) {
		return $sce.trustAsHtml(  listevents );		
	}
	// Manage the Modal 
	this.isshowDialog=false;
	this.openDialog = function()
	{
		this.isshowDialog=true;
	};
	this.closeDialog = function()
	{
		this.isshowDialog=false;
	}

	// -----------------------------------------------------------------------------------------
	// Thanks to Bonita to not implement the POST : we have to split the URL
	// -----------------------------------------------------------------------------------------
	var postParams=
	{
		"listUrlCall" : [],
		"action":"",
		"callbackfct ": null,
		"advPercent":0
		
	}
	this.sendPost = function(actionToServer,  param , callbackfct )
	{
		var self=this;
		self.inprogress=true;
		console.log("sendPost inProgress<=true action="+actionToServer+" Json="+ angular.toJson( param ));
		console.log("updateModelCallback marker ="+self.marker);

		var json = angular.toJson( param, false);

		self.postParams={};
		self.postParams.action= actionToServer;
		self.postParams.listUrlCall=[];
		self.postParams.callbackfct = callbackfct;
		var action = "collectReset";
		// split the string by packet of 1800 (URL cut at 2800, and we have
		// to encode the string)
		while (json.length>0)
		{
			var jsonSplit= json.substring(0,1500);
			var jsonEncodeSplit = encodeURIComponent( jsonSplit );
			
			// Attention, the char # is not encoded !!
			jsonEncodeSplit = jsonEncodeSplit.replace(new RegExp('#', 'g'), '%23');

			
			console.log("collectAdd JsonPartial="+jsonSplit);
			// console.log("collect_add JsonEncode ="+jsonEncodeSplit);
		
			
			self.postParams.listUrlCall.push( "action="+action+"&paramjsonpartial="+jsonEncodeSplit);
			action = "collectAdd";
			json = json.substring(1500);
		}
		self.postParams.listUrlCall.push( "action="+self.postParams.action);
		
		
		self.postParams.listUrlIndex=0;
		self.executeListUrl( self ) // , self.listUrlCall, self.listUrlIndex
									// );
		// this.operationTour('updateJob', plugtour, plugtour, true);
		// console.log("sendPost.END")
		
	}
	
	this.executeListUrl = function( self ) // , listUrlCall, listUrlIndex )
	{
		console.log("executeListUrl: "+(self.postParams.listUrlIndex+1) +"/"+ self.postParams.listUrlCall.length+" : "+self.postParams.listUrlCall[ self.postParams.listUrlIndex ]);
		self.postParams.advPercent= Math.round( (100 *  self.postParams.listUrlIndex) / self.postParams.listUrlCall.length);
		
		// console.log("executeListUrl call HTTP");

		$http.get( '?page=custompage_bookmobile&t='+Date.now()+'&'+self.postParams.listUrlCall[ self.postParams.listUrlIndex ], this.getHttpConfig() )
		.success( function ( jsonResult, statusHttp, headers, config ) {
			// connection is lost ?
			if (statusHttp==401 || typeof jsonResult === 'string') {
				console.log("Redirected to the login page ! statusHttp=" +statusHttp+" jsonResult=["+jsonResult+"]");
				window.location.reload();
			}
			// console.log("executeListUrl receive data HTTP");
			// console.log("Correct, advance one more",
			// angular.toJson(jsonResult));
			console.log("postResultSuccess marker ="+self.marker);

			self.postParams.listUrlIndex = self.postParams.listUrlIndex+1;
			if (self.postParams.listUrlIndex  < self.postParams.listUrlCall.length )
				self.executeListUrl( self );
			else
			{
				self.inprogress = false;
				self.postParams.advPercent= 100; 
				console.log("sendPost finish inProgress<=false jsonResult="+ angular.toJson(jsonResult));
				if (self.postParams.callbackfct) {
					self.postParams.callbackfct(  jsonResult, self  );
				} 
			}
		})
		.error( function(jsonResult, statusHttp, headers, config) {
			console.log("executeListUrl.error HTTP statusHttp="+statusHttp);
			// connection is lost ?
			if (statusHttp==401) {
				console.log("Redirected to the login page !");
				window.location.reload();
			}
			self.inprogress = false;				
			});	
	};
	// -----------------------------------------------------------------------------------------
	// tool
	// -----------------------------------------------------------------------------------------
	this.getHtml = function(listevents, sourceContext) {
		// console.log("getHtml:Start (r/o) source="+sourceContext);
		return $sce.trustAsHtml(listevents);
	}


	
});



})();