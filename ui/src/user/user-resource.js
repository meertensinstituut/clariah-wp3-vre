import Resource from "../common/resource";

const URL = process.env.REACT_APP_WHOAMI_ENDPOINT;

export default class UserResource {

    static async whoAmI() {
        const response = await fetch(URL);
        return Resource.validate(response);
    }
}