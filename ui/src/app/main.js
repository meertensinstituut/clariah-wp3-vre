import React from "react";
import Files from "../files/files";
import {Route, Switch} from "react-router-dom";
import Home from "./home";
import Deploy from "../deploy/deploy";

export default class Main extends React.Component {

    render() {
        return (
            <main>
                <Switch>
                    <Route exact path='/' component={Home}/>
                    <Route exact path='/files' component={Files}/>
                    <Route exact path='/deploy' component={Deploy}/>
                </Switch>
            </main>
        );
    }
}