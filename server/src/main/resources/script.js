function changeNewGroupVision() {
	if (document.getElementById("new-group").style.display == "none") {
		document.getElementById("new-group").style.display = "inline-block";
		document.getElementById("vision-button").innerHTML = "-";
	} else {
		document.getElementById("new-group").style.display = "none";
		document.getElementById("vision-button").innerHTML = "+";
	}
}

function addClientNode(id){
	var parent = document.getElementById(id);
	if (parent.childElementCount < 10) {
            var client = document.createElement("input");
            client.setAttribute("type", "text");
            client.setAttribute("name", "client");
	
            parent.appendChild(client);
	} else {
            document.getElementById("add-button_" + id).style.display = "none";
	}
}

function addFileNode(id){
	var parent = document.getElementById(id);
	if (parent.childElementCount < 10) {
            var client = document.createElement("input");
            client.setAttribute("type", "text");
            client.setAttribute("name", "file");
	
            parent.appendChild(client);
	} else {
            document.getElementById("add-button_" + id).style.display = "none";
	}
}
