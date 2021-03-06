import React from "react";
import Files from "../files/files";
import {Route, Switch, withRouter} from "react-router-dom";
import Home from "./home";
import Viewer from "../viewer/viewer";
import Editor from "../editor/editor";
import Deploy from "../deploy/deploy";
import Poll from "../poll/poll";
import ReactGui from "../react-gui";

class Main extends React.Component {

    render() {
        return (
            <main>
                <Switch>
                    <Route exact path='/' component={Home}/>
                    <Route exact path='/files' component={Files}/>
                    <Route exact path='/view/:objectId/:objectName' component={Viewer}/>
                    <Route exact path='/edit/:objectId/:objectName' component={Editor}/>
                    <Route exact path='/deploy' component={Deploy}/>
                    <Route exact path='/poll/:workDir' component={Poll}/>
                    <Route exact path='/react-gui/' component={ReactGui}/>
                </Switch>
            </main>
        );
    }
}

export default withRouter(Main);