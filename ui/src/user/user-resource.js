import $ from "jquery";

const URL = process.env.REACT_APP_WHOAMI_ENDPOINT;

export default class UserResource {

    static whoAmI() {
        return $.get(URL);
    }
}