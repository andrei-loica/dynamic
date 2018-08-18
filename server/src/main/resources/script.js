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
	var client = document.createElement("input");
	client.setAttribute("type", "text");
	client.setAttribute("name", "client");
	
	var parent = document.getElementById(id);
	parent.appendChild(client);
}

function addFileNode(id){
	var client = document.createElement("input");
	client.setAttribute("type", "text");
	client.setAttribute("name", "file");
	
	var parent = document.getElementById(id);
	parent.appendChild(client);
}
