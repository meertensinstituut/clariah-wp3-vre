export default class IllegalActionError extends Error {

    constructor(message, status) {
        super(message);
        this.name = "IllegalActionError";
        this.status = status;
    }

}