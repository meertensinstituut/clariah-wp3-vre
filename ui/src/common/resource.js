import RequestError from "./request-error";

export default class Resource {

    /**
     * Validate response
     * @throws RequestError when response is not ok
     * @return json
     */
    static async validate(response) {
        const body = await response.json();
        if (response.ok) {
            return body;
        }
        let message = "";
        if (body.message) {
            message = body.message;
        } else if (body.msg) {
            message = body.msg;
        } else if (body.error && body.error.message) {
            message = body.error.message;
        } else {
            message = response.statusText;
        }
        throw new RequestError(message, response.status);
    }

}