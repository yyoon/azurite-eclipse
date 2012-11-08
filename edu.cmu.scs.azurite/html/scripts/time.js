function Time(time) {
	this.time = new Date(time);
	// console.log(time);
	
}

Time.prototype.toString = function () {
    return (this.time.getHours()) + ":" + (this.time.getMinutes()) + ":" + (this.time.getSeconds()) + " " +
	(this.time.getMonth() + "/" + (this.time.getDate()) + "/" +(this.time.getFullYear()));
};
