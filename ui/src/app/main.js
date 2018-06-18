import React from "react";
import Objects from "./objects";
import {Route, Switch} from "react-router-dom";
import Home from "./home";

export default class Main extends React.Component {

    render() {
        return (
            <main>
                <Switch>
                    <Route exact path='/' component={Home}/>
                    <Route exact path='/files' component={Objects}/>
                </Switch>
            </main>
        );
    }
}