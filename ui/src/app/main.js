import React from "react";
import Files from "../files/files";
import {Route, Switch} from "react-router-dom";
import Home from "./home";

export default class Main extends React.Component {

    render() {
        return (
            <main>
                <Switch>
                    <Route exact path='/' component={Home}/>
                    <Route exact path='/files' component={Files}/>
                </Switch>
            </main>
        );
    }
}