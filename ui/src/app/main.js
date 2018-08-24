import React from "react";
import Files from "../files/files";
import {Route, Switch, withRouter} from "react-router-dom";
import Home from "./home";
import Viewer from "../viewer/viewer";
import Deploy from "../deploy/deploy";
import Poll from "../poll/poll";

class Main extends React.Component {

    render() {
        return (
            <main>
                <Switch>
                    <Route exact path='/' component={Home}/>
                    <Route exact path='/files' component={Files}/>
                    <Route exact path='/view/:objectId/:objectName' component={Viewer}/>
                    <Route exact path='/deploy' component={Deploy}/>
                    <Route exact path='/poll/:workDir' component={Poll} />
                </Switch>
            </main>
        );
    }
}

export default withRouter(Main);