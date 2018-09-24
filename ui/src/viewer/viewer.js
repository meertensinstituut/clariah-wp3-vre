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
        const params = {"params": [{"name": "input", "type": "file", "value": this.state.objectId}]};

        try {
            const data = await Switchboard.getViewers(this.state.objectId);
            const hasViewer = data.length > 0;
            if (!hasViewer) {
                throw Error("No viewer found for " + this.state.objectName);
            }
            const viewer = data[0].name;
            const deployData = await Switchboard.postDeployment(viewer, params);

            Switchboard.getDeploymentStatusResultWhen(deployData.workDir, DeploymentStatus.FINISHED)
                .fail((xhr) => {
                    this.setState({errorResponse: xhr.responseJSON})
                })
                .done((viewerData) => {
                    this.setState({
                        viewer: viewer,
                        viewerFileContent: {__html: viewerData.viewerFileContent},
                        viewerFileName: viewerData.viewerFile
                    });
                });
        } catch (e) {
            console.log(e);
            this.setState({errorResponse: {msg: e.message}})
        }

    }

    render() {
        const viewerFile = this.state.viewerFileContent
            ? <div dangerouslySetInnerHTML={this.state.viewerFileContent}/>
            : null;

        const spinner = !this.state.viewerFileContent && !this.state.errorResponse
            ? <Spinner response={this.state.errorResponse}/>
            : null;

        const usingViewer = this.state.viewer
            ? <span>With viewer: <code>{this.state.viewer}</code></span>
            : null;

        return (
            <div>
                <h1>Viewing file {this.state.objectName} </h1>
                <ErrorMsg response={this.state.errorResponse}/>
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
