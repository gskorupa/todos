var myData = {"todos": []};
var offline = true;
var serviceURL = "http://webapp-poc.mircloud.host/todos";

// get data from the service
function getData(elementId, statusId) {
    w3Http(serviceURL, function () {
        if (this.readyState === 4 && this.status === 200) {
            myData = JSON.parse(this.responseText);
            offline = false;
            updatePage(myData, elementId, statusId, false);
        } else if (this.readyState === 4 && this.status === 0) {
            //service not available, we will use data stored in myData variable
            offline = true;
            updatePage(myData, elementId, statusId, true);
        }
    });

}

// update application view
function updatePage(data, elementId, statusId, offlineStatus) {
    if (data.todos.length > 0) {
        w3DisplayData(elementId, data);
        document.getElementById(elementId).style.display = "";
    } else {
        document.getElementById(elementId).style.display = "none";
    }
    if (offlineStatus) {
        document.getElementById(statusId).innerHTML = "offline";
    } else {
        document.getElementById(statusId).innerHTML = "online";
    }
}

// submit application form data to the service
function AJAXSubmit(oFormElement, elementId, statusId, getdataCallback) {
    if (offline) {
        submitLocally(oFormElement);
        getdataCallback(elementId, statusId);
        return;
    }
    oFormElement.action = serviceURL;
    if (!oFormElement.action) {
        // action not defined
        return;
    }
    var oReq = new XMLHttpRequest();
    oReq.onreadystatechange = function () {
        if (this.readyState == 4 && this.status == 201) {
            getdataCallback(elementId, statusId);
        } else if (this.readyState == 4 && this.status == 0) {
           // error
        }
    };
    if (oFormElement.method.toLowerCase() === "post") {
        oReq.open("post", oFormElement.action);
        oReq.send(new FormData(oFormElement));
    } else {
        // error - not a POST method
    }
    oFormElement.reset();
}

// store application form data locally (if the service is not available)
function submitLocally(oFormElement) {
    var newToDo = {
        "name": oFormElement.elements["name"].value,
        "description": oFormElement.elements["description"].value
    };
    myData.todos.push(newToDo);
}

// not used, example to show how expected server response should look like
var exampleData = {"todos": [
        {"name": "my task 1", "description": "description 1"},
        {"name": "my task 2", "description": "description 2"}
    ]};

