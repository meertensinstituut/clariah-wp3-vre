import React from "react";
import {withRouter} from "react-router-dom";
import PropTypes from 'prop-types';
import Switchboard from "../common/switchboard";
import Spinner from "../common/spinner";
import ErrorMsg from "../common/error-msg";
import {DeploymentStatus} from "../common/deployment-status";

/**
 * Views a file using the first viewer
 * out of the list with found viewers.
 */
class Viewer extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            objectId: this.props.match.params.objectId,
            objectName: this.props.match.params.objectName,
            viewerFile: null,
            view: false
        };
        this.getViewOfObject(this.state);
    }

    async getViewOfObject() {
        const params = {
            "params": [{
                "name": "input",
                "type": "file",
                "value": this.state.objectId
            }]
        };
        try {
            const data = await Switchboard
                .getViewers(this.state.objectId)
                .catch((e) => this.setState({error: e}));
            const hasViewer = data.length > 0;
            if (!hasViewer) {
                this.setState({error: Error("No viewer found for " + this.state.objectName)});
            }
            const viewer = data[0].name;
            const deployData = await Switchboard
                .postDeployment(viewer, params)
                .catch((e) => this.setState({error: e}));
            const workDir = deployData.workDir;
            const finished = DeploymentStatus.FINISHED;
            const viewerData = await Switchboard
                .getDeploymentWhen(workDir, finished)
                .then(viewerData => viewerData)
                .catch((e) => this.setState({error: e}));
            this.setState({
                viewer: viewer,
                viewerFileContent: {__html: viewerData.viewerFileContent},
                viewerFileName: viewerData.viewerFile
            });
        } catch (e) {
            this.setState({error: {message: "Could not view file: " + e.message}})
        }

    }

    render() {
        if (this.state.error)
            return <ErrorMsg error={this.state.error}/>;

        const viewerFile = this.state.viewerFileContent
            ? <div dangerouslySetInnerHTML={this.state.viewerFileContent}/>
            : null;

        const spinner = !this.state.viewerFileContent && !this.state.error
            ? <Spinner response={this.state.error}/>
            : null;

        const usingViewer = this.state.viewer
            ? <span>With viewer: <code>{this.state.viewer}</code></span>
            : null;

        return (
            <div>
                <h1>Viewing file {this.state.objectName}</h1>
                <ErrorMsg response={this.state.error}/>
                <p>{usingViewer}</p>
                <div>{this.props.content}</div>
                {viewerFile}
                {spinner}
            </div>
        );
    }
}

Viewer.propTypes = {
    match: PropTypes.shape({
        params: PropTypes.shape({
            objectId: PropTypes.string.isRequired,
            objectName: PropTypes.string.isRequired
        })
    })
};

export default withRouter(Viewer);
