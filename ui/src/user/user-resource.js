import Resource from "../common/resource";
import {WHOAMI_ENDPOINT} from "../config";

export default class UserResource {

    static async whoAmI() {
        const response = await fetch(WHOAMI_ENDPOINT);
        return Resource.validate(response);
    }
}