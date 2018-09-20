import React from "react";
import $ from "jquery";

const URL = process.env.REACT_APP_WHOAMI_ENDPOINT;

export default class UserResource extends React.Component {

    static whoAmI() {
        return $.get(URL);
    }
}